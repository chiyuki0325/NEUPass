const viewportMeta = document.querySelector("meta[name=viewport]")
const viewportContent = viewportMeta.getAttribute("content")
viewportMeta.setAttribute("content", viewportContent.replaceAll("1.0", "0.8"))

// 重复注入时替换旧监听，避免一次点击触发多次收起。
window.removeEventListener("click", window.jwxtMobileMenuClickHandler)

// 委托处理动态菜单，只在点击无下拉箭头的二、三级菜单时收起侧栏。
window.jwxtMobileMenuClickHandler = (event) => {
    const menu = event.target.closest('[class*="menuLevel2___"], [class*="menuLevel3___"]')
    if (!menu || !menu.closest('[class*="sideMenuRoot___"]') || menu.querySelector('[class*="menuArrowDown___"]')) {
        return
    }

    // 等原菜单完成页面切换，再点击当前可见侧栏的“隐藏菜单”。
    setTimeout(() => {
        const hideButton = Array.from(document.querySelectorAll('[class*="sideMenuRoot___"] [class*="hideShowBtnContainer___"]'))
            .find(button => button.getClientRects().length > 0 && button.textContent.includes("隐藏菜单"))
        hideButton?.click()
    }, 0)
}

window.addEventListener("click", window.jwxtMobileMenuClickHandler)

// 将悬浮菜单入口改为“显示菜单”，同时处理切换页面后新生成的入口。
window.jwxtMobileMenuObserver?.disconnect()

const renameFloatingMenu = () => {
    document.querySelectorAll('[class*="sideMenuFloat___"]').forEach(button => {
        if (button.textContent !== "显示菜单") {
            button.textContent = "显示菜单"
        }
    })
}

renameFloatingMenu()
window.jwxtMobileMenuObserver = new MutationObserver(renameFloatingMenu)
window.jwxtMobileMenuObserver.observe(document.body, { childList: true, subtree: true, characterData: true })
