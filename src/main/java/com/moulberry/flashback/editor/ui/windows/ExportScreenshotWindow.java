package com.moulberry.flashback.editor.ui.windows;

import com.moulberry.flashback.Flashback;
import com.moulberry.flashback.Utils;
import com.moulberry.flashback.combo_options.VideoCodec;
import com.moulberry.flashback.combo_options.VideoContainer;
import com.moulberry.flashback.configuration.FlashbackConfig;
import com.moulberry.flashback.editor.ui.ImGuiHelper;
import com.moulberry.flashback.exporting.AsyncFileDialogs;
import com.moulberry.flashback.exporting.ExportJob;
import com.moulberry.flashback.exporting.ExportSettings;
import com.moulberry.flashback.state.EditorScene;
import com.moulberry.flashback.state.EditorState;
import com.moulberry.flashback.state.EditorStateManager;
import imgui.ImGui;
import imgui.flag.ImGuiWindowFlags;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

import java.nio.file.Path;

public class ExportScreenshotWindow {

    private static boolean open = false;
    private static boolean close = false;

    public static void render() {
        if (open) {
            open = false;
            ImGui.openPopup("###导出截图");
        }

        if (ImGuiHelper.beginPopupModalCloseable("导出截图###导出截图", ImGuiWindowFlags.AlwaysAutoResize)) {
            if (close) {
                close = false;
                ImGui.closeCurrentPopup();
                ImGuiHelper.endPopupModalCloseable();
                return;
            }

            FlashbackConfig config = Flashback.getConfig();
            config.forceDefaultExportSettings.apply(config);

            if (config.resolution == null || config.resolution.length != 2) {
                config.resolution = new int[]{1920, 1080};
            }

            ImGuiHelper.inputInt("分辨率", config.resolution);
            config.resolution[0] = Math.max(1, config.resolution[0]);
            config.resolution[1] = Math.max(1, config.resolution[1]);

            EditorState editorState = EditorStateManager.getCurrent();

            if (ImGui.checkbox("超级采样抗锯齿", config.ssaa)) {
                config.ssaa = !config.ssaa;
            }
            ImGuiHelper.tooltip("超级采样抗锯齿：通过以双倍分辨率渲染游戏并缩小尺寸来消除锯齿边缘");

            ImGui.sameLine();

            if (ImGui.checkbox("无GUI", config.noGui)) {
                config.noGui = !config.noGui;
            }
            ImGuiHelper.tooltip("删除屏幕上的所有 UI，仅渲染世界");

            if (editorState != null && !editorState.replayVisuals.renderSky) {
                if (ImGui.checkbox("透明的天空", config.transparentBackground)) {
                    config.transparentBackground = !config.transparentBackground;
                }
            }

            if (editorState != null && ImGui.button("截屏")) {
                String defaultName = StartExportWindow.getDefaultFilename(null, "png", config);
                String defaultExportPathString = config.defaultExportPath;

                AsyncFileDialogs.saveFileDialog(defaultExportPathString, defaultName, "PNG", "png").thenAccept(pathStr -> {
                    if (pathStr != null) {
                        Path path = Path.of(pathStr);
                        config.defaultExportPath = path.getParent().toString();

                        LocalPlayer player = Minecraft.getInstance().player;
                        int tick = Flashback.getReplayServer().getReplayTick();

                        boolean transparent = config.transparentBackground && !editorState.replayVisuals.renderSky;
                        boolean ssaa = config.ssaa;
                        boolean noGui = config.noGui;

                        EditorState copiedEditorState = editorState.copy();
                        for (EditorScene scene : copiedEditorState.scenes) {
                            scene.keyframeTracks.clear();
                        }

                        ExportSettings settings = new ExportSettings(null, copiedEditorState,
                            player.position(), player.getYRot(), player.getXRot(),
                            config.resolution[0], config.resolution[1], tick, tick,
                            1, false, VideoContainer.PNG_SEQUENCE, null, null, 0, transparent, ssaa, noGui,
                            false, false, null,
                            path);

                        close = true;
                        Utils.exportSequenceCount += 1;
                        Flashback.EXPORT_JOB = new ExportJob(settings);
                    }
                });
            }

            ImGuiHelper.endPopupModalCloseable();
        }

        close = false;
    }

    public static void open() {
        open = true;
    }

}
