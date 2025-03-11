package com.moulberry.flashback.editor.ui.windows;

import com.moulberry.flashback.Flashback;
import com.moulberry.flashback.keyframe.Keyframe;
import com.moulberry.flashback.keyframe.KeyframeType;
import com.moulberry.flashback.keyframe.impl.CameraShakeKeyframe;
import com.moulberry.flashback.keyframe.impl.FOVKeyframe;
import com.moulberry.flashback.keyframe.impl.TimeOfDayKeyframe;
import com.moulberry.flashback.playback.ReplayServer;
import com.moulberry.flashback.record.FlashbackMeta;
import com.moulberry.flashback.state.EditorScene;
import com.moulberry.flashback.state.EditorState;
import com.moulberry.flashback.state.EditorSceneHistoryAction;
import com.moulberry.flashback.state.EditorSceneHistoryEntry;
import com.moulberry.flashback.state.EditorStateManager;
import com.moulberry.flashback.state.KeyframeTrack;
import com.moulberry.flashback.visuals.ReplayVisuals;
import com.moulberry.flashback.combo_options.Sizing;
import com.moulberry.flashback.editor.ui.ImGuiHelper;
import imgui.ImGui;
import net.minecraft.client.Minecraft;

import java.util.List;

public class VisualsWindow {

    private static final float[] floatBuffer = new float[]{0};
    private static final int[] intBuffer = new int[]{0};

    public static void render() {
        ReplayServer replayServer = Flashback.getReplayServer();
        if (replayServer == null) {
            return;
        }

        FlashbackMeta metadata = replayServer.getMetadata();
        EditorState editorState = EditorStateManager.get(metadata.replayIdentifier);
        ReplayVisuals visuals = editorState.replayVisuals;

        if (ImGui.begin("视觉效果")) {
            ImGuiHelper.separatorWithText("GUI");

            if (ImGui.checkbox("聊天", visuals.showChat)) {
                visuals.showChat = !visuals.showChat;
                editorState.markDirty();
            }
            if (ImGui.checkbox("Boss 血条", visuals.showBossBar)) {
                visuals.showBossBar = !visuals.showBossBar;
                editorState.markDirty();
            }
            if (ImGui.checkbox("标题文本", visuals.showTitleText)) {
                visuals.showTitleText = !visuals.showTitleText;
                editorState.markDirty();
            }
            if (ImGui.checkbox("记分牌", visuals.showScoreboard)) {
                visuals.showScoreboard = !visuals.showScoreboard;
                editorState.markDirty();
            }
            if (ImGui.checkbox("物品栏", visuals.showActionBar)) {
                visuals.showActionBar = !visuals.showActionBar;
                editorState.markDirty();
            }

            if (Minecraft.getInstance().cameraEntity != null && Minecraft.getInstance().cameraEntity != Minecraft.getInstance().player) {
                if (ImGui.checkbox("快捷栏", visuals.showHotbar)) {
                    visuals.showHotbar = !visuals.showHotbar;
                    editorState.markDirty();
                }
            }

            ImGuiHelper.separatorWithText("世界");

            if (ImGui.checkbox("渲染方块", visuals.renderBlocks)) {
                visuals.renderBlocks = !visuals.renderBlocks;
                editorState.markDirty();
            }

            if (ImGui.checkbox("渲染实体", visuals.renderEntities)) {
                visuals.renderEntities = !visuals.renderEntities;
                editorState.markDirty();
            }

            if (ImGui.checkbox("渲染玩家", visuals.renderPlayers)) {
                visuals.renderPlayers = !visuals.renderPlayers;
                editorState.markDirty();
            }

            if (ImGui.checkbox("渲染粒子", visuals.renderParticles)) {
                visuals.renderParticles = !visuals.renderParticles;
                editorState.markDirty();
            }

            if (ImGui.checkbox("渲染天空", visuals.renderSky)) {
                visuals.renderSky = !visuals.renderSky;
                editorState.markDirty();
            }

            if (!visuals.renderSky) {
                if (ImGui.colorButton("天空颜色", visuals.skyColour)) {
                    ImGui.openPopup("##EditSkyColour");
                }
                ImGui.sameLine();
                ImGui.text("天空颜色");

                if (ImGui.beginPopup("##EditSkyColour")) {
                    ImGui.colorPicker3("天空颜色", visuals.skyColour);
                    ImGui.endPopup();
                }
            }

            if (ImGui.checkbox("渲染名牌", visuals.renderNametags)) {
                visuals.renderNametags = !visuals.renderNametags;
                editorState.markDirty();
            }

            ImGuiHelper.separatorWithText("覆写设置");

            // Fog distance
            if (ImGui.checkbox("覆写雾", visuals.overrideFog)) {
                visuals.overrideFog = !visuals.overrideFog;
                editorState.markDirty();
            }
            if (visuals.overrideFog) {
                floatBuffer[0] = visuals.overrideFogStart;
                if (ImGui.sliderFloat("开始", floatBuffer, 0.0f, 512.0f)) {
                    visuals.overrideFogStart = floatBuffer[0];
                    editorState.markDirty();
                }

                floatBuffer[0] = visuals.overrideFogEnd;
                if (ImGui.sliderFloat("结束", floatBuffer, 0.0f, 512.0f)) {
                    visuals.overrideFogEnd = floatBuffer[0];
                    editorState.markDirty();
                }
            }

            if (ImGui.checkbox("覆写雾颜色", visuals.overrideFogColour)) {
                visuals.overrideFogColour = !visuals.overrideFogColour;
                editorState.markDirty();
            }
            if (visuals.overrideFogColour) {
                if (ImGui.colorButton("雾颜色", visuals.fogColour)) {
                    ImGui.openPopup("##EditFogColour");
                }
                ImGui.sameLine();
                ImGui.text("雾颜色");

                if (ImGui.beginPopup("##EditFogColour")) {
                    ImGui.colorPicker3("雾颜色", visuals.fogColour);
                    ImGui.endPopup();
                }
            }

            // FOV
            if (ImGui.checkbox("覆写视野", visuals.overrideFov)) {
                visuals.overrideFov = !visuals.overrideFov;
                Minecraft.getInstance().levelRenderer.needsUpdate();
                editorState.markDirty();
            }
            if (visuals.overrideFov) {
                ImGui.sameLine();
                if (ImGui.smallButton("+")) {
                    addKeyframe(editorState, replayServer, new FOVKeyframe(visuals.overrideFovAmount));
                }
                ImGuiHelper.tooltip("添加视野关键帧");

                floatBuffer[0] = visuals.overrideFovAmount;
                if (ImGui.sliderFloat("视野", floatBuffer, 1.0f, 110.0f, "%.1f")) {
                    visuals.setFov(floatBuffer[0]);
                    editorState.markDirty();
                }
            }

            // Time
            if (ImGui.checkbox("覆写世界时间", visuals.overrideTimeOfDay >= 0)) {
                if (visuals.overrideTimeOfDay >= 0) {
                    visuals.overrideTimeOfDay = -1;
                } else {
                    visuals.overrideTimeOfDay = (int)(Minecraft.getInstance().level.getDayTime() % 24000);
                }
                editorState.markDirty();
            }
            if (visuals.overrideTimeOfDay >= 0) {
                ImGui.sameLine();
                if (ImGui.smallButton("+")) {
                    addKeyframe(editorState, replayServer, new TimeOfDayKeyframe((int) visuals.overrideTimeOfDay));
                }
                ImGuiHelper.tooltip("添加世界时间关键帧");

                intBuffer[0] = (int) visuals.overrideTimeOfDay;
                if (ImGui.sliderInt("世界时间", intBuffer, 0, 24000)) {
                    visuals.overrideTimeOfDay = intBuffer[0];
                    editorState.markDirty();
                }
            }

            // Night vision
            if (ImGui.checkbox("夜视", visuals.overrideNightVision)) {
                visuals.overrideNightVision = !visuals.overrideNightVision;
            }

            // Camera shake
            if (ImGui.checkbox("摄像机抖动", visuals.overrideCameraShake)) {
                visuals.overrideCameraShake = !visuals.overrideCameraShake;
                editorState.markDirty();
            }
            if (visuals.overrideCameraShake) {
                ImGui.sameLine();
                if (ImGui.smallButton("+")) {
                    if (visuals.cameraShakeSplitParams) {
                        addKeyframe(editorState, replayServer, new CameraShakeKeyframe(visuals.cameraShakeXFrequency, visuals.cameraShakeXAmplitude,
                            visuals.cameraShakeYFrequency, visuals.cameraShakeYAmplitude, true));
                    } else {
                        float frequency = (visuals.cameraShakeXFrequency + visuals.cameraShakeYFrequency)/2.0f;
                        float amplitude = (visuals.cameraShakeXAmplitude + visuals.cameraShakeYAmplitude)/2.0f;
                        addKeyframe(editorState, replayServer, new CameraShakeKeyframe(frequency, amplitude, frequency, amplitude, false));
                    }
                }
                ImGuiHelper.tooltip("添加摄像机抖动关键帧");

                if (ImGui.checkbox("分离 Y/X", visuals.cameraShakeSplitParams)) {
                    visuals.cameraShakeSplitParams = !visuals.cameraShakeSplitParams;
                    editorState.markDirty();
                }

                if (visuals.cameraShakeSplitParams) {
                    ImGui.setNextItemWidth(100);
                    floatBuffer[0] = visuals.cameraShakeXFrequency;
                    if (ImGui.sliderFloat("频率 X", floatBuffer, 0.1f, 10.0f, "%.1f")) {
                        visuals.cameraShakeXFrequency = floatBuffer[0];
                        editorState.markDirty();
                    }

                    ImGui.setNextItemWidth(100);
                    floatBuffer[0] = visuals.cameraShakeXAmplitude;
                    if (ImGui.sliderFloat("幅度 X", floatBuffer, 0.0f, 10.0f, "%.1f")) {
                        visuals.cameraShakeXAmplitude = floatBuffer[0];
                        editorState.markDirty();
                    }

                    ImGui.setNextItemWidth(100);
                    floatBuffer[0] = visuals.cameraShakeYFrequency;
                    if (ImGui.sliderFloat("频率 Y", floatBuffer, 0.1f, 10.0f, "%.1f")) {
                        visuals.cameraShakeYFrequency = floatBuffer[0];
                        editorState.markDirty();
                    }

                    ImGui.setNextItemWidth(100);
                    floatBuffer[0] = visuals.cameraShakeYAmplitude;
                    if (ImGui.sliderFloat("幅度 Y", floatBuffer, 0.0f, 10.0f, "%.1f")) {
                        visuals.cameraShakeYAmplitude = floatBuffer[0];
                        editorState.markDirty();
                    }
                } else {
                    ImGui.setNextItemWidth(100);
                    floatBuffer[0] = (visuals.cameraShakeXFrequency + visuals.cameraShakeYFrequency)/2.0f;
                    if (ImGui.sliderFloat("频率", floatBuffer, 0.1f, 10.0f, "%.1f")) {
                        visuals.cameraShakeXFrequency = floatBuffer[0];
                        visuals.cameraShakeYFrequency = floatBuffer[0];
                        editorState.markDirty();
                    }

                    ImGui.setNextItemWidth(100);
                    floatBuffer[0] = (visuals.cameraShakeXAmplitude + visuals.cameraShakeYAmplitude)/2.0f;
                    if (ImGui.sliderFloat("幅度", floatBuffer, 0.0f, 10.0f, "%.1f")) {
                        visuals.cameraShakeXAmplitude = floatBuffer[0];
                        visuals.cameraShakeYAmplitude = floatBuffer[0];
                        editorState.markDirty();
                    }
                }
            }

            // Camera Roll
            if (ImGui.checkbox("摄像机旋转", visuals.overrideRoll)) {
                visuals.overrideRoll = !visuals.overrideRoll;
                Minecraft.getInstance().levelRenderer.needsUpdate();
                editorState.markDirty();
            }
            if (visuals.overrideRoll) {
                floatBuffer[0] = visuals.overrideRollAmount;
                if (ImGui.sliderFloat("旋转", floatBuffer, -180.0f, 180.0f, "%.1f")) {
                    visuals.overrideRollAmount = floatBuffer[0];
                    Minecraft.getInstance().levelRenderer.needsUpdate();
                    editorState.markDirty();
                }
            }

            visuals.overrideWeatherMode = ImGuiHelper.enumCombo("天气", visuals.overrideWeatherMode);

            ImGuiHelper.separatorWithText("其他");

            if (ImGui.checkbox("三分线", visuals.ruleOfThirdsGuide)) {
                visuals.ruleOfThirdsGuide = !visuals.ruleOfThirdsGuide;
                editorState.markDirty();
            }

            if (ImGui.checkbox("十字线", visuals.centerGuide)) {
                visuals.centerGuide = !visuals.centerGuide;
                editorState.markDirty();
            }

            if (ImGui.checkbox("摄像机路径", visuals.cameraPath)) {
                visuals.cameraPath = !visuals.cameraPath;
                editorState.markDirty();
            }

            visuals.sizing = ImGuiHelper.enumCombo("缩放", visuals.sizing);
            if (visuals.sizing == Sizing.CHANGE_ASPECT_RATIO) {
                visuals.changeAspectRatio = ImGuiHelper.enumCombo("长宽比", visuals.changeAspectRatio);
                editorState.markDirty();
            }

            if (!editorState.hideDuringExport.isEmpty()) {
                if (ImGui.button("取消隐藏所有实体")) {
                    editorState.hideDuringExport.clear();
                    editorState.markDirty();
                }
            }
        }
        ImGui.end();
    }

    private static void addKeyframe(EditorState editorState, ReplayServer replayServer, Keyframe keyframe) {
        KeyframeType<?> keyframeType = keyframe.keyframeType();
        EditorScene scene = editorState.currentScene();

        // Try add to existing enabled keyframe track
        for (int i = 0; i < scene.keyframeTracks.size(); i++) {
            KeyframeTrack keyframeTrack = scene.keyframeTracks.get(i);
            if (keyframeTrack.enabled && keyframeTrack.keyframeType == keyframeType) {
                scene.setKeyframe(i, replayServer.getReplayTick(), keyframe);
                return;
            }
        }

        // Try add to any keyframe track
        for (int i = 0; i < scene.keyframeTracks.size(); i++) {
            KeyframeTrack keyframeTrack = scene.keyframeTracks.get(i);
            if (keyframeTrack.keyframeType == keyframeType) {
                scene.setKeyframe(i, replayServer.getReplayTick(), keyframe);
                return;
            }
        }

        String description = "添加 " + keyframeType.name() + " 帧";
        int newKeyframeTrackIndex = scene.keyframeTracks.size();
        scene.push(new EditorSceneHistoryEntry(
            List.of(new EditorSceneHistoryAction.RemoveTrack(keyframeType, newKeyframeTrackIndex)),
            List.of(
                new EditorSceneHistoryAction.AddTrack(keyframeType, newKeyframeTrackIndex),
                new EditorSceneHistoryAction.SetKeyframe(keyframeType, newKeyframeTrackIndex, replayServer.getReplayTick(), keyframe)
            ),
            description
        ));
        editorState.markDirty();
    }
}
