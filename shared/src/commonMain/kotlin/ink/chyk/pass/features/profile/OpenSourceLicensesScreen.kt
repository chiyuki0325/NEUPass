package ink.chyk.pass.features.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.tencent.tmm.kmmresource.compose.fileTextResource
import com.tencent.tmm.kmmresource.compose.painterResource
import com.tencent.tmm.kmmresource.compose.stringResource
import ink.chyk.pass.resources.MR
import ink.chyk.pass.ui.platformScaffoldContentWindowInsets
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private val licenseCatalogJson = Json { ignoreUnknownKeys = true }

@Composable
fun OpenSourceLicensesScreen(onBack: () -> Unit) {
  val resourceText = fileTextResource(MR.files.open_source_licenses)
  val catalog = remember(resourceText) {
    licenseCatalogJson.decodeFromString<LicenseCatalog>(resourceText)
  }
  var selected by remember { mutableStateOf<LicensedLibrary?>(null) }
  var query by remember { mutableStateOf("") }
  val libraries = remember(catalog, query) {
    catalog.libraries.filter {
      it.name.contains(query, ignoreCase = true) || it.uniqueId.contains(query, ignoreCase = true)
    }
  }
  val uriHandler = LocalUriHandler.current

  Scaffold(contentWindowInsets = platformScaffoldContentWindowInsets()) { innerPadding ->
    LazyColumn(
      modifier = Modifier.fillMaxSize().padding(innerPadding),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      item {
        Spacer(Modifier.height(32.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(onClick = onBack) {
            Icon(
              painterResource(MR.images.ic_fluent_chevron_left_20_filled),
              stringResource(MR.strings.license_back),
            )
          }
          Text(
            stringResource(MR.strings.open_source_licenses),
            style = MaterialTheme.typography.headlineLarge,
          )
        }
        Text(
          stringResource(MR.strings.open_source_licenses_description),
          modifier = Modifier.padding(vertical = 16.dp),
          style = MaterialTheme.typography.bodyMedium,
        )
        OutlinedTextField(
          value = query,
          onValueChange = { query = it },
          label = { Text(stringResource(MR.strings.license_search)) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
      }
      items(libraries, key = { it.uniqueId }) { library ->
        Card(onClick = { selected = library }, modifier = Modifier.fillMaxWidth()) {
          Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(library.name, style = MaterialTheme.typography.titleMedium)
            Text(library.uniqueId, style = MaterialTheme.typography.bodySmall)
            Text(library.artifactVersion, style = MaterialTheme.typography.bodySmall)
            Text(
              library.licenses.mapNotNull { catalog.licenses[it]?.name }.joinToString(" / "),
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    }
  }

  selected?.let { library ->
    AlertDialog(
      onDismissRequest = { selected = null },
      title = { Text(library.name) },
      text = {
        SelectionContainer {
          Column(
            Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
          ) {
            if (library.description.isNotBlank()) Text(library.description)
            library.notices.forEach { Text(it) }
            library.licenses.mapNotNull { catalog.licenses[it] }.forEach { license ->
              Text(license.name, style = MaterialTheme.typography.titleSmall)
              Text(license.content, style = MaterialTheme.typography.bodySmall)
            }
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { selected = null }) {
          Text(stringResource(MR.strings.license_close))
        }
      },
      dismissButton = {
        if (library.website.isNotBlank()) {
          TextButton(onClick = { uriHandler.openUri(library.website) }) {
            Text(stringResource(MR.strings.license_project_source))
          }
        }
      },
    )
  }
}

@Serializable
internal data class LicenseCatalog(
  val libraries: List<LicensedLibrary>,
  val licenses: Map<String, LicenseDocument>,
)

@Serializable
internal data class LicensedLibrary(
  val uniqueId: String,
  val name: String,
  val artifactVersion: String = "",
  val website: String = "",
  val description: String = "",
  val licenses: List<String> = emptyList(),
  val notices: List<String> = emptyList(),
)

@Serializable
internal data class LicenseDocument(
  val name: String,
  val content: String,
)
