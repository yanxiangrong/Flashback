package com.moulberry.flashback.editor.ui.windows;

import com.moulberry.flashback.Flashback;
import com.moulberry.flashback.combo_options.VideoContainer;
import com.moulberry.flashback.configuration.FlashbackConfig;
import com.moulberry.flashback.editor.ui.ImGuiHelper;
import com.moulberry.flashback.exporting.AsyncFileDialogs;
import com.moulberry.flashback.exporting.ExportJob;
import com.moulberry.flashback.exporting.ExportSettings;
import com.moulberry.flashback.keyframe.interpolation.InterpolationType;
import com.moulberry.flashback.state.EditorState;
import com.moulberry.flashback.state.EditorStateManager;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImShort;
import imgui.type.ImString;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

import java.nio.file.Path;

public class PreferencesWindow {

    private static boolean open = false;
    private static boolean close = false;

    public static void render() {
        if (open) {
            open = false;
            ImGui.openPopup("###偏好设置");
        }

        boolean wasOpen = ImGui.isPopupOpen("###偏好设置");

        ImVec2 center = ImGui.getMainViewport().getCenter();
        ImGui.setNextWindowPos(center.x, center.y, ImGuiCond.Appearing, 0.5f, 0.5f);
        ImGui.setNextWindowSize(400, 0);
        if (ImGuiHelper.beginPopupModalCloseable("偏好设置###偏好设置", ImGuiWindowFlags.NoResize)) {
            if (close) {
                close = false;
                ImGui.closeCurrentPopup();
                ImGuiHelper.endPopupModalCloseable();
                return;
            }

            FlashbackConfig config = Flashback.getConfig();

            // Exporting
            ImGuiHelper.separatorWithText("导出");

            ImString imString = ImGuiHelper.createResizableImString(config.defaultExportFilename);
            ImGui.setNextItemWidth(200);
            if (ImGui.inputText("导出文件名", imString)) {
                config.defaultExportFilename = ImGuiHelper.getString(imString);
                config.delayedSaveToDefaultFolder();
            }
            ImGuiHelper.tooltip("导出时的默认文件名\n变量:\n\t%date%\tyear-month-day\n\t%time%\thh_mm_ss\n\t%replay%\t回放名\n\t%seq%\t此会话的导出计数");

            // Keyframes
            ImGuiHelper.separatorWithText("关键帧");

            ImGui.setNextItemWidth(200);
            config.defaultInterpolationType = ImGuiHelper.enumCombo("默认插值", config.defaultInterpolationType);

            if (ImGui.collapsingHeader("高级")) {
                ImGui.textWrapped("除非您知道自己在做什么，否则请勿更改其中任何一项！！如果您更改其中一项然后在网上到处问，你就是SB！！");
                if (ImGui.checkbox("Disable increased first-person updates", config.disableIncreasedFirstPersonUpdates)) {
                    config.disableIncreasedFirstPersonUpdates = !config.disableIncreasedFirstPersonUpdates;
                    config.delayedSaveToDefaultFolder();
                }
                if (ImGui.checkbox("Disable third-person cancel", config.disableThirdPersonCancel)) {
                    config.disableThirdPersonCancel = !config.disableThirdPersonCancel;
                    config.delayedSaveToDefaultFolder();
                }
            }

            ImGuiHelper.endPopupModalCloseable();
        }

        if (wasOpen && !ImGui.isPopupOpen("###偏好设置")) {
            Flashback.getConfig().saveToDefaultFolder();
        }

        close = false;
    }

    public static void open() {
        open = true;
    }

}
