import base64
import hashlib
import io
import json
import os
import re
import subprocess
import tarfile
import tomllib
import xml.etree.ElementTree as ElementTree
import zipfile
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
CACHE = ROOT / "build/license-sources"
OUTPUT = ROOT / "shared/src/commonMain/resources/MR/files/open_source_licenses.json"
WEBVIEW_REVISION = "58314a9dc161a9892a5ecc2b2c0b3a593217c258"
LEGAL_FILENAME = re.compile(
    r"^(licen[cs]e|notice|copying|copyright|authors|ofl)([._-].*)?$",
    re.IGNORECASE,
)


def download(url):
    destination = CACHE / hashlib.sha256(url.encode()).hexdigest()
    if not destination.exists():
        destination.parent.mkdir(parents=True, exist_ok=True)
        temporary = destination.with_suffix(".tmp")
        subprocess.run(
            [
                "curl", "--fail", "--location", "--silent", "--show-error",
                "--retry", "2", "--connect-timeout", "15", "--max-time", "180",
                url, "--output", str(temporary),
            ],
            check=True,
        )
        temporary.replace(destination)
    return destination.read_bytes()


def source_text(source):
    content = download(source["url"])
    if source.get("base64"):
        content = base64.b64decode(content)
    if "member" in source:
        with zipfile.ZipFile(io.BytesIO(content)) as archive:
            content = archive.read(source["member"])
    text = content.decode("utf-8-sig").strip()
    if not text or text.lstrip().lower().startswith(("<!doctype", "<html")):
        raise ValueError(f"Invalid license text: {source['url']}")
    return text


def add_document(documents, name, content):
    key = hashlib.sha256(content.encode()).hexdigest()
    documents.setdefault(key, {"name": name, "content": content})
    return key


def load_reports():
    libraries = {}
    documents = {}
    artifacts = []
    for module in ("shared", "androidApp", "desktopApp"):
        report = ROOT / module / "build/generated/aboutLibraries/aboutlibraries.json"
        data = json.loads(report.read_text())
        for library in data["libraries"]:
            libraries[library["uniqueId"]] = library
        documents.update(data["licenses"])
        artifacts.extend(
            json.loads((ROOT / module / "build/reports/licenses/artifacts.json").read_text())
        )
    return libraries, documents, artifacts


def metadata_for(artifact, metadata):
    identifier = artifact["id"]
    candidates = [
        identifier,
        re.sub(r"-android-debug$", "-android", identifier),
        re.sub(r"-(android-debug|android|jvm|desktop)$", "", identifier),
    ]
    if identifier.startswith("org.jetbrains.skiko:skiko-awt-runtime-"):
        candidates.append("org.jetbrains.skiko:skiko-awt")
    for candidate in candidates:
        if candidate in metadata:
            return metadata[candidate]
    version_directory = Path(artifact["path"]).parent.parent
    pom_files = sorted(version_directory.glob("*/*.pom"))
    if pom_files:
        pom = ElementTree.parse(pom_files[0])
        namespace = {"m": "http://maven.apache.org/POM/4.0.0"}
        name = pom.findtext("m:name", namespaces=namespace) or identifier
        website = pom.findtext("m:url", namespaces=namespace) or ""
        for library in metadata.values():
            if library.get("name") == name and library.get("website") == website:
                return library
        licenses = []
        for license_node in pom.findall("m:licenses/m:license", namespace):
            license_name = license_node.findtext("m:name", namespaces=namespace) or ""
            url = license_node.findtext("m:url", namespaces=namespace) or ""
            combined = (license_name + " " + url).lower()
            if "apache" in combined:
                licenses.append("Apache-2.0")
            elif "mit" in combined:
                licenses.append("MIT")
            elif "bsd" in combined and "3" in combined:
                licenses.append("BSD-3-Clause")
            else:
                raise ValueError(f"Unmapped POM license: {identifier} / {license_name}")
        if licenses:
            return {
                "uniqueId": identifier,
                "name": name,
                "website": website,
                "licenses": licenses,
                "description": pom.findtext("m:description", namespaces=namespace) or "",
            }
    raise ValueError(f"Missing dependency metadata: {identifier}")


def embedded_notices(path):
    if not zipfile.is_zipfile(path):
        return []
    notices = []
    with zipfile.ZipFile(path) as archive:
        for member in archive.namelist():
            if member.endswith("/"):
                continue
            if LEGAL_FILENAME.match(Path(member).name):
                content = archive.read(member).decode("utf-8-sig", errors="replace").strip()
                if content:
                    notices.append(content)
            elif member.endswith(".jar"):
                with zipfile.ZipFile(io.BytesIO(archive.read(member))) as nested:
                    for entry in nested.namelist():
                        if LEGAL_FILENAME.match(Path(entry).name):
                            content = nested.read(entry).decode(
                                "utf-8-sig", errors="replace",
                            ).strip()
                            if content:
                                notices.append(content)
    return list(dict.fromkeys(notices))


def gradle_libraries(metadata, documents, artifacts, overrides):
    result = {}
    for artifact in artifacts:
        original = metadata_for(artifact, metadata)
        identifier = original["uniqueId"]
        override = next(
            (item for item in overrides if identifier.startswith(item["prefix"])), {},
        )
        library = result.setdefault(
            identifier,
            {
                "uniqueId": identifier,
                "name": original.get("name") or identifier,
                "artifactVersion": artifact["version"],
                "website": override.get("website") or original.get("website") or "",
                "description": original.get("description") or "",
                "licenses": override.get("licenses", original.get("licenses", [])),
                "notices": [],
                "artifacts": [],
            },
        )
        coordinate = f"{artifact['id']}:{artifact['version']}"
        if coordinate not in library["artifacts"]:
            library["artifacts"].append(coordinate)
        for notice in embedded_notices(artifact["path"]):
            key = add_document(documents, "随包许可证与版权声明", notice)
            if key not in library["licenses"]:
                library["licenses"].append(key)
        for source in override.get("sources", []):
            source = {**source, "url": source["url"].replace("{version}", artifact["version"])}
            content = source_text(source)
            key = add_document(documents, source["name"], content)
            if key not in library["licenses"]:
                library["licenses"].append(key)
        for developer in original.get("developers", []):
            attribution = developer.get("name", "")
            if attribution and attribution not in library["notices"]:
                library["notices"].append(attribution)
    return result


def rust_package(package):
    name, version = package["name"], package["version"]
    registry = Path(os.environ.get("CARGO_HOME", Path.home() / ".cargo")) / "registry"
    registries = [registry, Path.home() / ".cache/cargo/registry"]
    for candidate in registries:
        matches = list((candidate / "src").glob(f"*/{name}-{version}/Cargo.toml"))
        if matches:
            root = matches[0].parent
            manifest = tomllib.loads(matches[0].read_text())["package"]
            vcs_file = root / ".cargo_vcs_info.json"
            if vcs_file.exists():
                manifest["vcs"] = json.loads(vcs_file.read_text())
            texts = [
                path.read_text(errors="replace").strip()
                for path in sorted(root.rglob("*"))
                if path.is_file() and LEGAL_FILENAME.match(path.name)
            ]
            return package, manifest, list(dict.fromkeys(texts))
    url = f"https://static.crates.io/crates/{name}/{name}-{version}.crate"
    with tarfile.open(fileobj=io.BytesIO(download(url)), mode="r:gz") as archive:
        manifest = tomllib.loads(
            archive.extractfile(f"{name}-{version}/Cargo.toml").read().decode()
        )["package"]
        vcs_path = f"{name}-{version}/.cargo_vcs_info.json"
        if vcs_path in archive.getnames():
            manifest["vcs"] = json.loads(archive.extractfile(vcs_path).read())
        texts = [
            archive.extractfile(member).read().decode("utf-8-sig", errors="replace").strip()
            for member in archive.getmembers()
            if member.isfile() and LEGAL_FILENAME.match(Path(member.name).name)
        ]
    return package, manifest, list(dict.fromkeys(texts))


def upstream_rust_licenses(manifest):
    repository = manifest.get("repository", "").removesuffix(".git").rstrip("/")
    if not repository.startswith("https://github.com/"):
        return []
    repository = repository.removeprefix("https://github.com/")
    revision = manifest.get("vcs", {}).get("git", {}).get("sha1")
    url = f"https://api.github.com/repos/{repository}/contents"
    if revision:
        url += f"?ref={revision}"
    entries = json.loads(download(url))
    texts = [
        download(entry["download_url"]).decode("utf-8-sig").strip()
        for entry in entries
        if entry["type"] == "file" and LEGAL_FILENAME.match(entry["name"])
    ]
    if not texts and revision:
        return upstream_rust_licenses({**manifest, "vcs": {}})
    return texts


def rust_libraries(documents, overrides):
    lock_url = (
        "https://raw.githubusercontent.com/NucleusFramework/ComposeNativeWebview/"
        f"{WEBVIEW_REVISION}/wrywebview/Cargo.lock"
    )
    lock = tomllib.loads(download(lock_url).decode())
    packages = [
        package for package in lock["package"]
        if package.get("source", "").startswith("registry+")
    ]
    libraries = {}
    with ThreadPoolExecutor(max_workers=6) as executor:
        for package, manifest, texts in executor.map(rust_package, packages):
            name, version = package["name"], package["version"]
            expression = manifest.get("license", "")
            override = overrides.get(name, {})
            if not texts and not override:
                texts = upstream_rust_licenses(manifest)
            if not texts and not override:
                raise ValueError(f"Missing Rust license text: {name} {version} ({expression})")
            keys = [
                add_document(documents, f"{name} — {expression}", text)
                for text in texts if text
            ]
            keys.extend(override.get("licenses", []))
            identifier = f"cargo:{name}:{version}"
            libraries[identifier] = {
                "uniqueId": identifier,
                "name": f"{name}（WebView原生依赖）",
                "artifactVersion": version,
                "website": manifest.get("repository") or f"https://crates.io/crates/{name}",
                "description": (
                    f"上游许可表达式：{expression}。\n"
                    "来自WebView对应版本的Cargo.lock，包含跨平台及构建期依赖；"
                    "具体发行包使用其中与目标平台及启用功能相符的部分。\n"
                    f"对应版本源码：https://crates.io/api/v1/crates/{name}/{version}/download"
                ),
                "licenses": keys,
                "notices": manifest.get("authors", []) + override.get("notices", []),
            }
    return libraries


def supplemental_libraries(config, documents):
    libraries = {}
    for item in config["libraries"]:
        library = {
            key: value for key, value in item.items()
            if key != "sources"
        }
        library.setdefault("licenses", [])
        for source in item.get("sources", []):
            library["licenses"].append(
                add_document(documents, source["name"], source_text(source)),
            )
        libraries[library["uniqueId"]] = library
    return libraries


def main():
    config = json.loads((ROOT / "scripts/licenses/sources.json").read_text())
    metadata, documents, artifacts = load_reports()
    for artifact in artifacts:
        expected = config["verifiedVersions"].get(metadata_for(artifact, metadata)["uniqueId"])
        if expected and artifact["version"] != expected:
            raise ValueError(f"Native license inventory needs updating: {artifact['id']} {artifact['version']}")
    for identifier, source in config["licenseTexts"].items():
        documents[identifier] = {
            "name": source["name"],
            "content": source_text(source),
        }
    libraries = gradle_libraries(metadata, documents, artifacts, config["overrides"])
    libraries.update(supplemental_libraries(config, documents))
    libraries.update(rust_libraries(documents, config["rustOverrides"]))
    used = set()
    for library in libraries.values():
        if not library.get("licenses"):
            raise ValueError(f"Missing license: {library['uniqueId']}")
        for key in library["licenses"]:
            if not documents.get(key, {}).get("content"):
                raise ValueError(f"Missing license text: {library['uniqueId']} / {key}")
            used.add(key)
        library["description"] = library.get("description", "").strip()
        if library.get("artifacts"):
            library["description"] += "\n\n组件：\n" + "\n".join(sorted(library.pop("artifacts")))
    catalog = {
        "libraries": sorted(libraries.values(), key=lambda entry: entry["name"].casefold()),
        "licenses": {
            key: {"name": documents[key]["name"], "content": documents[key]["content"]}
            for key in sorted(used)
        },
    }
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    OUTPUT.write_text(json.dumps(catalog, ensure_ascii=False, indent=2) + "\n")
    print(f"Generated {OUTPUT.relative_to(ROOT)}: {len(libraries)} entries, {len(used)} documents")


if __name__ == "__main__":
    main()
