package com.moulberry.flashback.editor.ui.windows;

import imgui.ImGui;
import imgui.ImGuiViewport;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImBoolean;

public class ExportDoneWindow {

    public static boolean exportDoneWindowOpen = false;

    public static void render() {
        if (!exportDoneWindowOpen) {
            return;
        }

        ImGuiViewport viewport = ImGui.getMainViewport();
        ImGui.setNextWindowPos(viewport.getCenterX(), viewport.getCenterY(), ImGuiCond.Appearing, 0.5f, 0.5f);

        ImBoolean open = new ImBoolean(true);

        ImGui.setNextWindowSizeConstraints(250, 50, 5000, 5000);
        int flags = ImGuiWindowFlags.AlwaysAutoResize | ImGuiWindowFlags.NoDocking | ImGuiWindowFlags.NoSavedSettings;

        ImGui.openPopup("###ExportDone");
        if (ImGui.beginPopupModal("导出完成###ExportDone", open, flags)) {
            ImGui.text("导出成功完成");
            ImGui.endPopup();
        }

        if (!open.get()) {
            exportDoneWindowOpen = false;
        }
    }

}
