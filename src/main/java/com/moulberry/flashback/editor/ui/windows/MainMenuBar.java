package com.moulberry.flashback.editor.ui.windows;

import com.moulberry.flashback.Flashback;
import com.moulberry.flashback.configuration.FlashbackConfig;
import com.moulberry.flashback.exporting.ExportJobQueue;
import com.moulberry.flashback.screen.select_replay.SelectReplayScreen;
import imgui.ImGui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;

import java.nio.file.Files;
import java.nio.file.Path;

public class MainMenuBar {

    public static void render() {
        if (ImGui.beginMainMenuBar()) {
            renderInner();
            ImGui.endMainMenuBar();
        }
    }

    public static void renderInner() {
        FlashbackConfig config = Flashback.getConfig();

        if (ImGui.beginMenu("文件")) {
            if (ImGui.menuItem("导出视频")) {
                StartExportWindow.open();
            }
            if (!ExportJobQueue.queuedJobs.isEmpty()) {
                String name = "导出队列 (" + ExportJobQueue.count() + ")";
                if (ImGui.menuItem(name + "###队列任务")) {
                    ExportQueueWindow.open();
                }
            }
            if (ImGui.menuItem("导出截屏")) {
                ExportScreenshotWindow.open();
            }
            ImGui.separator();
            if (ImGui.menuItem("打开回放")) {
                Flashback.openReplayFromFileBrowser();
            }
            if (!config.recentReplays.isEmpty()) {
                if (ImGui.beginMenu("打开最近")) {
                    Path replayFolder = Flashback.getReplayFolder();
                    for (String recentReplay : config.recentReplays) {
                        Path path = Path.of(recentReplay);

                        if (Files.exists(path)) {
                            String display = path.toString();

                            try {
                                Path relative = replayFolder.relativize(path);
                                String relativeStr = relative.toString();
                                if (!relativeStr.contains("..")) {
                                    display = relativeStr;
                                }
                            } catch (Exception ignored) {}

                            if (ImGui.menuItem(display)) {
                                Flashback.openReplayWorld(path);
                                break;
                            }
                        }
                    }
                    ImGui.endMenu();
                }
            }
            if (ImGui.menuItem("退出回放")) {
                Minecraft minecraft = Minecraft.getInstance();
                if (minecraft.level != null) {
                    minecraft.level.disconnect();
                }
                minecraft.disconnect();
                minecraft.setScreen(new SelectReplayScreen(new TitleScreen(), Flashback.getReplayFolder()));
            }
            ImGui.endMenu();
        }
        if (ImGui.menuItem("偏好设置")) {
            PreferencesWindow.open();
        }

        ImGui.separator();

        if (ImGui.menuItem("玩家列表")) {
            toggleWindow("player_list");
        }
        if (ImGui.menuItem("动作")) {
            toggleWindow("movement");
        }
        if (ImGui.menuItem("渲染过滤器")) {
            toggleWindow("render_filter");
        }

        ImGui.separator();

        if (ImGui.menuItem("隐藏 (F1)")) {
            Minecraft.getInstance().options.hideGui = true;
        }
    }

    private static void toggleWindow(String windowName) {
        var openedWindows = Flashback.getConfig().openedWindows;
        boolean playerListIsOpen = openedWindows.contains(windowName);
        if (playerListIsOpen) {
            openedWindows.remove(windowName);
        } else {
            openedWindows.add(windowName);
        }
        Flashback.getConfig().delayedSaveToDefaultFolder();
    }

}
