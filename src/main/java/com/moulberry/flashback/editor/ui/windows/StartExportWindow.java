package com.moulberry.flashback.editor.ui.windows;

import com.mojang.blaze3d.platform.Window;
import com.moulberry.flashback.Flashback;
import com.moulberry.flashback.Utils;
import com.moulberry.flashback.combo_options.AspectRatio;
import com.moulberry.flashback.combo_options.AudioCodec;
import com.moulberry.flashback.combo_options.Sizing;
import com.moulberry.flashback.combo_options.VideoCodec;
import com.moulberry.flashback.combo_options.VideoContainer;
import com.moulberry.flashback.configuration.FlashbackConfig;
import com.moulberry.flashback.editor.ui.ReplayUI;
import com.moulberry.flashback.exporting.ExportJobQueue;
import com.moulberry.flashback.state.EditorScene;
import com.moulberry.flashback.state.EditorState;
import com.moulberry.flashback.state.EditorStateManager;
import com.moulberry.flashback.editor.ui.ImGuiHelper;
import com.moulberry.flashback.exporting.AsyncFileDialogs;
import com.moulberry.flashback.exporting.ExportJob;
import com.moulberry.flashback.exporting.ExportSettings;
import com.moulberry.flashback.playback.ReplayServer;
import imgui.ImGui;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImString;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.FileUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class StartExportWindow {

    private static boolean open = false;
    private static boolean close = false;

    private static final int[] lastFramebufferSize = new int[]{0, 0};
    private static AspectRatio lastCustomAspectRatio = null;

    private static final int[] startEndTick = new int[]{0, 100};

    private static VideoContainer[] supportedContainers = null;
    private static VideoContainer[] supportedContainersWithTransparency = null;

    private static final ImString bitrate = ImGuiHelper.createResizableImString("20m");

    private static final ImString jobName = ImGuiHelper.createResizableImString("");

    private static String installedIncompatibleModsString = null;
    private static final List<String> potentialIncompatibleMods = List.of(
        "g4mespeed", // causes rendering issues due to overriding partial tick time
        "feather" // causes miscellaneous crashes and issues that are impossible to debug
    );

    static {
        bitrate.inputData.allowedChars = "0123456789kmb";
    }

    public static void render() {
        EditorState editorState = EditorStateManager.getCurrent();

        if (open) {
            installedIncompatibleModsString = null;
            for (String potentialIncompatibleMod : potentialIncompatibleMods) {
                if (FabricLoader.getInstance().isModLoaded(potentialIncompatibleMod)) {
                    if (installedIncompatibleModsString == null) {
                        installedIncompatibleModsString = potentialIncompatibleMod;
                    } else {
                        installedIncompatibleModsString += ", " + potentialIncompatibleMod;
                    }
                }
            }

            FlashbackConfig config = Flashback.getConfig();
            config.forceDefaultExportSettings.apply(config);

            if (config.resolution == null || config.resolution.length != 2) {
                config.resolution = new int[]{1920, 1080};
            }
            if (config.framerate == null || config.framerate.length != 1) {
                config.framerate = new float[]{60};
            }
            if (config.selectedVideoEncoder == null || config.selectedVideoEncoder.length != 1) {
                config.selectedVideoEncoder = new int[]{0};
            }
            if (config.audioCodec == null) {
                config.audioCodec = AudioCodec.AAC;
            }

            ImGui.openPopup("###StartExport");

            if (editorState != null && editorState.replayVisuals.sizing == Sizing.CHANGE_ASPECT_RATIO) {
                AspectRatio aspectRatio = editorState.replayVisuals.changeAspectRatio;
                if (aspectRatio != null && aspectRatio != lastCustomAspectRatio) {
                    switch (aspectRatio) {
                        case ASPECT_16_9 -> {
                            config.resolution[0] = 1920;
                            config.resolution[1] = 1080;
                        }
                        case ASPECT_9_16 -> {
                            config.resolution[0] = 1080;
                            config.resolution[1] = 1920;
                        }
                        case ASPECT_240_1 -> {
                            config.resolution[0] = 1920;
                            config.resolution[1] = 800;
                        }
                        case ASPECT_1_1 -> {
                            config.resolution[0] = 1920;
                            config.resolution[1] = 1920;
                        }
                        case ASPECT_4_3 -> {
                            config.resolution[0] = 1600;
                            config.resolution[1] = 1200;
                        }
                        case ASPECT_3_2 -> {
                            config.resolution[0] = 2160;
                            config.resolution[1] = 1440;
                        }
                    }
                }
                lastCustomAspectRatio = aspectRatio;
            }

            open = false;
        }

        if (ImGuiHelper.beginPopupModalCloseable("导出到视频###StartExport", ImGuiWindowFlags.AlwaysAutoResize)) {
            if (close) {
                close = false;
                ImGui.closeCurrentPopup();
                ImGuiHelper.endPopupModalCloseable();
                return;
            }

            FlashbackConfig config = Flashback.getConfig();

            ImGuiHelper.separatorWithText("录制选项");

            ImGuiHelper.inputInt("分辨率", config.resolution);

            if (config.resolution[0] < 16) config.resolution[0] = 16;
            if (config.resolution[1] < 16) config.resolution[1] = 16;
            if (config.resolution[0] % 2 != 0) config.resolution[0] += 1;
            if (config.resolution[1] % 2 != 0) config.resolution[1] += 1;

            if (ImGuiHelper.inputInt("开始/结束 帧", startEndTick)) {
                ReplayServer replayServer = Flashback.getReplayServer();
                if (editorState != null && replayServer != null) {
                    editorState.currentScene().setExportTicks(startEndTick[0], startEndTick[1], replayServer.getTotalReplayTicks());
                    editorState.markDirty();
                }
            }
            ImGuiHelper.inputFloat("帧率", config.framerate);

            if (ImGui.checkbox("重置 RNG", config.resetRng)) {
                config.resetRng = !config.resetRng;
            }
            ImGuiHelper.tooltip("尝试消除重放中的随机性，以便在多次录制同一场景时产生更一致的输出");

            ImGui.sameLine();

            if (ImGui.checkbox("超级采样抗锯齿", config.ssaa)) {
                config.ssaa = !config.ssaa;
            }
            ImGuiHelper.tooltip("超级采样抗锯齿：通过以双倍分辨率渲染游戏并缩小尺寸来消除锯齿边缘");

            ImGui.sameLine();

            if (ImGui.checkbox("无 GUI", config.noGui)) {
                config.noGui = !config.noGui;
            }
            ImGuiHelper.tooltip("删除屏幕上的所有 UI，仅渲染世界");

            ImGuiHelper.separatorWithText("视频选项");

            renderVideoOptions(editorState, config);

            AudioCodec[] supportedAudioCodecs = config.container.getSupportedAudioCodecs();
            if (supportedAudioCodecs.length > 0) {
                ImGuiHelper.separatorWithText("音频选项");

                if (ImGui.checkbox("录制音频", config.recordAudio)) {
                    config.recordAudio = !config.recordAudio;
                }

                if (config.recordAudio) {
                    if (ImGui.checkbox("立体声 (2 通道)", config.stereoAudio)) {
                        config.stereoAudio = !config.stereoAudio;
                    }

                    AudioCodec newAudioCodec = ImGuiHelper.enumCombo("音频编码器", config.audioCodec, supportedAudioCodecs);
                    if (newAudioCodec != config.audioCodec) {
                        config.audioCodec = newAudioCodec;
                    }

                    if (editorState != null && editorState.audioSourceEntity != null) {
                        ImGui.text("音频源: \n实体(" + editorState.audioSourceEntity + ")");
                    } else {
                        ImGui.text("音频源: 摄像机");
                    }
                }
            } else {
                config.recordAudio = false;
            }

            if (installedIncompatibleModsString != null) {
                ImGuiHelper.separatorWithText("不兼容的模组");
                ImGui.textWrapped("您安装了一些已知会导致崩溃/渲染问题的模组。\n" +
                    "如果您在导出时遇到问题，请尝试删除以下模块：");
                ImGui.pushTextWrapPos();
                ImGui.textColored(0xFF0000FF, installedIncompatibleModsString);
                ImGui.popTextWrapPos();
            }

            ImGui.dummy(0, 10 * ReplayUI.getUiScale());

            float buttonSize = (ImGui.getContentRegionAvailX() - ImGui.getStyle().getItemSpacingX()) / 2f;
            if (ImGui.button("开始导出", buttonSize, ReplayUI.scaleUi(25))) {
                createExportSettings(null, config).thenAccept(settings -> {
                    if (settings != null) {
                        close = true;
                        Utils.exportSequenceCount += 1;
                        Flashback.EXPORT_JOB = new ExportJob(settings);
                        config.delayedSaveToDefaultFolder();
                    }
                });
            }
            ImGui.sameLine();
            if (ImGui.button("添加到队列", buttonSize, ReplayUI.scaleUi(25))) {
                jobName.set("任务 #" + (ExportJobQueue.count()+1));
                ImGui.openPopup("QueuedJobName");
            }

            if (ImGui.beginPopup("QueuedJobName")) {
                ImGui.setNextItemWidth(100);
                ImGui.inputText("任务名", jobName);

                if (ImGui.button("队列任务")) {
                    createExportSettings(ImGuiHelper.getString(jobName), config).thenAccept(settings -> {
                        if (settings != null) {
                            close = true;
                            Utils.exportSequenceCount += 1;
                            ExportJobQueue.queuedJobs.add(settings);
                        }
                    });
                }
                ImGui.sameLine();
                if (ImGui.button("返回")) {
                    ImGui.closeCurrentPopup();
                }
                ImGui.endPopup();
            }

            ImGuiHelper.endPopupModalCloseable();
        }

        close = false;
    }

    private static void renderVideoOptions(EditorState editorState, FlashbackConfig config) {
        if (editorState != null && !editorState.replayVisuals.renderSky) {
            if (ImGui.checkbox("透明的天空", config.transparentBackground)) {
                config.transparentBackground = !config.transparentBackground;
            }
        } else {
            config.transparentBackground = false;
        }

        VideoContainer[] containers;
        if (config.transparentBackground) {
            if (supportedContainersWithTransparency == null) {
                supportedContainersWithTransparency = VideoContainer.findSupportedContainers(true);
            }
            containers = supportedContainersWithTransparency;
        } else {
            if (supportedContainers == null) {
                supportedContainers = VideoContainer.findSupportedContainers(false);
            }
            containers = supportedContainers;
        }

        if (containers.length == 0) {
            ImGui.text("未找到受支持的容器格式");
            return;
        }

        if (config.container == null || !Arrays.asList(containers).contains(config.container)) {
            config.container = containers[0];
        }

        config.container = ImGuiHelper.enumCombo("容器格式", config.container, containers);

        if (config.container == VideoContainer.PNG_SEQUENCE) {
            return;
        }

        VideoCodec[] codecs = config.container.getSupportedVideoCodecs(config.transparentBackground);

        if (codecs.length == 0) {
            ImGui.text("未找到支持的编解码器");
            return;
        }

        if (config.videoCodec == null || !Arrays.asList(codecs).contains(config.videoCodec)) {
            config.videoCodec = codecs[0];
        }

        if (codecs.length > 1) {
            VideoCodec newCodec = ImGuiHelper.enumCombo("编解码器", config.videoCodec, codecs);
            if (newCodec != config.videoCodec) {
                config.videoCodec = newCodec;
                config.selectedVideoEncoder[0] = 0;
            }
        }

        String[] encoders = config.videoCodec.getEncoders();
        if (encoders.length > 1) {
            ImGuiHelper.combo("编码器", config.selectedVideoEncoder, encoders);
        }

        if (config.videoCodec != VideoCodec.GIF) {
            if (ImGui.checkbox("使用最大比特率", config.useMaximumBitrate)) {
                config.useMaximumBitrate = !config.useMaximumBitrate;
            }
            if (!config.useMaximumBitrate) {
                ImGui.inputText("比特率", bitrate);
                if (ImGui.isItemDeactivatedAfterEdit()) {
                    int numBitrate = stringToBitrate(ImGuiHelper.getString(bitrate));
                    bitrate.set(bitrateToString(numBitrate));
                }
            }
        } else {
            ImGui.pushTextWrapPos();
            ImGui.textColored(0xFFFFFFFF, "警告：GIF 输出可能非常大。请确保在导出之前了解 GIF 格式的限制。您最好使用 WebP，这是一种类似但更好的格式");
            ImGui.popTextWrapPos();
        }
    }

    private static CompletableFuture<ExportSettings> createExportSettings(@Nullable String name, FlashbackConfig config) {
        int numBitrate;
        if (config.useMaximumBitrate) {
            numBitrate = 0;
        } else {
            numBitrate = stringToBitrate(ImGuiHelper.getString(bitrate));
        }

        String defaultName = getDefaultFilename(name, config.container.extension(), config);

        Function<String, ExportSettings> callback = pathStr -> {
            if (pathStr != null) {
                int start = Math.max(0, startEndTick[0]);
                int end = Math.max(start, startEndTick[1]);

                ReplayServer replayServer = Flashback.getReplayServer();
                if (replayServer != null) {
                    int totalTicks = replayServer.getTotalReplayTicks();
                    start = Math.min(start, totalTicks);
                    end = Math.min(end, totalTicks);
                }

                EditorState editorState = EditorStateManager.getCurrent();
                if (editorState == null) {
                    return null;
                }

                LocalPlayer player = Minecraft.getInstance().player;
                if (player == null) {
                    return null;
                }

                boolean transparent = config.transparentBackground && !editorState.replayVisuals.renderSky;
                String encoder = config.videoCodec.getEncoders()[config.selectedVideoEncoder[0]];

                VideoCodec useVideoCodec = config.videoCodec;
                AudioCodec useAudioCodec = config.audioCodec;
                boolean shouldRecordAudio = config.recordAudio;

                if (config.container == VideoContainer.PNG_SEQUENCE) {
                    useVideoCodec = null;
                    encoder = null;
                    shouldRecordAudio = false;
                }

                if (!shouldRecordAudio) {
                    useAudioCodec = null;
                }

                Path path = Path.of(pathStr);
                config.defaultExportPath = path.getParent().toString();
                return new ExportSettings(name, editorState.copy(),
                    player.position(), player.getYRot(), player.getXRot(),
                    config.resolution[0], config.resolution[1], start, end,
                    Math.max(1, config.framerate[0]), config.resetRng, config.container, useVideoCodec, encoder, numBitrate, transparent, config.ssaa, config.noGui,
                    shouldRecordAudio, config.stereoAudio, useAudioCodec,
                    path);
            }

            return null;
        };

        String defaultExportPathString = config.defaultExportPath;
        if (config.container == VideoContainer.PNG_SEQUENCE) {
            return AsyncFileDialogs.openFolderDialog(defaultExportPathString).thenApply(callback);
        } else {
            return AsyncFileDialogs.saveFileDialog(defaultExportPathString, defaultName,
                config.container.extension(), config.container.extension()).thenApply(callback);
        }

    }

    public static @NotNull String getDefaultFilename(@Nullable String name, String extension, FlashbackConfig config) {
        Path defaultPath = FabricLoader.getInstance().getGameDir();

        try {
            if (config.defaultExportPath == null || config.defaultExportPath.isBlank() || !Files.exists(Path.of(config.defaultExportPath))) {
                config.defaultExportPath = defaultPath.toString();
            } else {
                defaultPath = Path.of(config.defaultExportPath);
            }
        } catch (Exception ignored) {}

        String defaultName = null;
        if (name != null) {
            try {
                defaultName = FileUtil.findAvailableName(defaultPath, name, "." + extension);
            } catch (Exception ignored) {}
        }
        if (defaultName == null) {
            String desiredName = Utils.resolveFilenameTemplate(Flashback.getConfig().defaultExportFilename);
            try {
                defaultName = FileUtil.findAvailableName(defaultPath, desiredName, "." + extension);
            } catch (Exception ignored) {}
        }
        if (defaultName == null) {
            String desiredName = Utils.resolveFilenameTemplate(Flashback.getConfig().defaultExportFilename);
            defaultName = desiredName + "." + extension;
        }
        return defaultName;
    }

    private static int stringToBitrate(String string) {
        int number = 0;
        int modifier = 1;
        int total = 0;

        for (char c : string.toCharArray()) {
            if (c >= '0' && c <= '9') {
                if (modifier > 1) {
                    total += number * modifier;
                    number = 0;
                    modifier = 1;
                }

                number *= 10;
                number += c - '0';
            } else if (c == 'k') {
                modifier *= 1000;
            } else if (c == 'm') {
                modifier *= 1000000;
            } else if (c == 'b') {
                modifier *= 1000000000;
            }
        }

        total += number * modifier;

        return total;
    }

    private static String bitrateToString(int bitrate) {
        if (bitrate >= 1000000000 && bitrate == (bitrate/1000000000)*1000000000) {
            return (bitrate/1000000000) + "b";
        } else if (bitrate >= 1000000 && bitrate == (bitrate/1000000)*1000000) {
            return (bitrate/1000000) + "m";
        } else if (bitrate >= 1000 && bitrate == (bitrate/1000)*1000) {
            return (bitrate/1000) + "k";
        } else {
            return String.valueOf(bitrate);
        }
    }

    public static void open() {
        open = true;

        EditorState editorState = EditorStateManager.getCurrent();
        EditorScene editorScene = editorState == null ? null : editorState.currentScene();

        if (editorScene != null && editorScene.exportStartTicks >= 0) {
            startEndTick[0] = editorScene.exportStartTicks;
        } else {
            startEndTick[0] = 0;
        }
        if (editorScene != null && editorScene.exportEndTicks >= 0) {
            startEndTick[1] = editorScene.exportEndTicks;
        } else {
            ReplayServer replayServer = Flashback.getReplayServer();
            if (replayServer == null) {
                startEndTick[1] = startEndTick[0] + 100;
            } else {
                startEndTick[1] = replayServer.getTotalReplayTicks();
            }
        }
    }

}
