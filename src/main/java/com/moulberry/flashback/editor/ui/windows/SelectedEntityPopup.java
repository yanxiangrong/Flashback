package com.moulberry.flashback.editor.ui.windows;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.yggdrasil.ProfileResult;
import com.moulberry.flashback.FilePlayerSkin;
import com.moulberry.flashback.Flashback;
import com.moulberry.flashback.Utils;
import com.moulberry.flashback.editor.ui.ImGuiHelper;
import com.moulberry.flashback.exporting.AsyncFileDialogs;
import com.moulberry.flashback.state.EditorState;
import imgui.ImGui;
import imgui.type.ImString;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Team;

import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class SelectedEntityPopup {

    private static ImString changeSkinInput = ImGuiHelper.createResizableImString("");
    static {
        changeSkinInput.inputData.allowedChars = "0123456789abcdef-";
    }
    private static ImString changeNameInput = ImGuiHelper.createResizableImString("");

    public static void open(Entity entity, EditorState editorState) {
        String nameOverride = editorState.nameOverride.get(entity.getUUID());
        if (nameOverride != null) {
            changeNameInput.set(nameOverride);
        } else {
            changeNameInput.set("");
        }

        GameProfile skinOverride = editorState.skinOverride.get(entity.getUUID());
        if (skinOverride != null) {
            changeSkinInput.set(skinOverride.getId().toString());
        } else {
            changeSkinInput.set("");
        }
    }

    public static void render(Entity entity, EditorState editorState) {
        UUID uuid = entity.getUUID();
        ImGui.text("实体: " + uuid);

        ImGui.separator();

        if (ImGui.button("查看")) {
            Minecraft.getInstance().cameraEntity.lookAt(EntityAnchorArgument.Anchor.EYES, entity.getEyePosition());
        }
        ImGui.sameLine();
        if (ImGui.button("观看")) {
            Minecraft.getInstance().player.connection.sendUnsignedCommand("spectate " + entity.getUUID());
            ImGui.closeCurrentPopup();
        }
        if (uuid.equals(editorState.audioSourceEntity)) {
            if (ImGui.button("取消设置音频源")) {
                editorState.audioSourceEntity = null;
                editorState.markDirty();
            }
        } else if (ImGui.button("设置音频源")) {
            editorState.audioSourceEntity = entity.getUUID();
            editorState.markDirty();
        }
        boolean isHiddenDuringExport = editorState.hideDuringExport.contains(entity.getUUID());
        if (ImGui.checkbox("导出时隐藏", isHiddenDuringExport)) {
            if (isHiddenDuringExport) {
                editorState.hideDuringExport.remove(entity.getUUID());
            } else {
                editorState.hideDuringExport.add(entity.getUUID());
            }
            editorState.markDirty();
        }

        if (!isHiddenDuringExport && entity instanceof Player player) {
            boolean hideNametag = editorState.hideNametags.contains(entity.getUUID());
            if (ImGui.checkbox("渲染名牌", !hideNametag)) {
                if (hideNametag) {
                    editorState.hideNametags.remove(entity.getUUID());
                } else {
                    editorState.hideNametags.add(entity.getUUID());
                }
            }

            if (!hideNametag) {
                boolean changedName = ImGui.inputTextWithHint("名字##SetNameInput", player.getScoreboardName(), changeNameInput);

                if (changedName) {
                    String string = ImGuiHelper.getString(changeNameInput);
                    if (string.isEmpty()) {
                        editorState.nameOverride.remove(entity.getUUID());
                    } else {
                        editorState.nameOverride.put(entity.getUUID(), string);
                    }
                }

                if (editorState.hideTeamPrefix.contains(player.getUUID())) {
                    if (ImGui.checkbox("隐藏队伍前缀", true)) {
                        editorState.hideTeamPrefix.remove(player.getUUID());
                    }
                } else {
                    PlayerTeam team = player.getTeam();
                    if (team != null && !Utils.isComponentEmpty(team.getPlayerPrefix())) {
                        if (ImGui.checkbox("隐藏队伍前缀", false)) {
                            editorState.hideTeamPrefix.add(player.getUUID());
                        }
                    }
                }

                if (editorState.hideTeamSuffix.contains(player.getUUID())) {
                    if (ImGui.checkbox("隐藏队伍后缀", true)) {
                        editorState.hideTeamSuffix.remove(player.getUUID());
                    }
                } else {
                    PlayerTeam team = player.getTeam();
                    if (team != null && !Utils.isComponentEmpty(team.getPlayerSuffix())) {
                        if (ImGui.checkbox("隐藏队伍后缀", false)) {
                            editorState.hideTeamSuffix.add(player.getUUID());
                        }
                    }
                }
            }

            ImGuiHelper.separatorWithText("更换皮肤和披风 (UUID)");
            ImGui.setNextItemWidth(320);
            ImGui.inputTextWithHint("##SetSkinInput", "e.g. d0e05de7-6067-454d-beae-c6d19d886191", changeSkinInput);

            if (!changeSkinInput.isEmpty()) {
                String string = ImGuiHelper.getString(changeSkinInput);
                try {
                    UUID changeSkinUuid = UUID.fromString(string);
                    if (ImGui.button("通过 UUID 应用皮肤")) {
                        ProfileResult profile = Minecraft.getInstance().getMinecraftSessionService().fetchProfile(changeSkinUuid, true);
                        editorState.skinOverride.put(entity.getUUID(), profile.profile());
                        editorState.skinOverrideFromFile.remove(entity.getUUID());
                    }
                } catch (Exception ignored) {}
            }

            if (ImGui.button("从文件上传皮肤")) {
                Path gameDir = FabricLoader.getInstance().getGameDir();
                CompletableFuture<String> future = AsyncFileDialogs.openFileDialog(gameDir.toString(),
                    "皮肤", "png");
                future.thenAccept(pathStr -> {
                    if (pathStr != null) {
                        editorState.skinOverride.remove(entity.getUUID());
                        editorState.skinOverrideFromFile.put(entity.getUUID(), new FilePlayerSkin(pathStr));
                    }
                });
            }

            if (editorState.skinOverride.containsKey(entity.getUUID()) || editorState.skinOverrideFromFile.containsKey(entity.getUUID())) {
                if (ImGui.button("重置皮肤")) {
                    editorState.skinOverride.remove(entity.getUUID());
                    editorState.skinOverrideFromFile.remove(entity.getUUID());
                    changeSkinInput.set("");
                }
            }
        }

//        ImGui.sameLine();
//        ImGui.button("Track Entity");
//
//        ImGui.checkbox("Force Glowing", false);
//        ImGui.sameLine();
//        ImGui.colorButton("Glow Colour", new float[4]);
//        ImGui.sameLine();
//        ImGui.text("Glow Colour");
//
//        if (entity instanceof LivingEntity) {
//            ImGui.checkbox("Show Nametag", true);
//            ImGui.checkbox("Override Nametag", false);
//        }
//        if (entity instanceof Player) {
//            ImGui.checkbox("Override Skin", false);
//        }
    }

}
