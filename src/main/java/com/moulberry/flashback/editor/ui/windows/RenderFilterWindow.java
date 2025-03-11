package com.moulberry.flashback.editor.ui.windows;

import com.moulberry.flashback.Flashback;
import com.moulberry.flashback.configuration.FlashbackConfig;
import com.moulberry.flashback.editor.ui.ImGuiHelper;
import com.moulberry.flashback.state.EditorState;
import com.moulberry.flashback.state.EditorStateManager;
import imgui.ImGui;
import imgui.ImGuiListClipper;
import imgui.ImGuiViewport;
import imgui.callback.ImListClipperCallback;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImBoolean;
import imgui.type.ImString;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RenderFilterWindow {

    private static boolean wasDocked = false;

    private static ImString entitySearch = ImGuiHelper.createResizableImString("");
    private static String lastEntitySearch = null;
    private static List<EntityType<?>> searchedEntityTypes = new ArrayList<>();

    private static ImString particleSearch = ImGuiHelper.createResizableImString("");
    private static String lastParticleSearch = null;
    private static List<ResourceLocation> searchedParticleTypes = new ArrayList<>();

    public static void render(ImBoolean open, boolean newlyOpened) {
        if (newlyOpened) {
            ImGuiViewport viewport = ImGui.getMainViewport();
            ImGui.setNextWindowPos(viewport.getCenterX(), viewport.getCenterY(), ImGuiCond.Appearing, 0.5f, 0.5f);
        }

        ImGui.setNextWindowSizeConstraints(250, 50, 5000, 5000);
        int flags = ImGuiWindowFlags.NoFocusOnAppearing;
        if (!wasDocked) {
            flags |= ImGuiWindowFlags.AlwaysAutoResize;
        }
        if (ImGui.begin("渲染过滤器###Render Filter", open, flags)) {
            wasDocked = ImGui.isWindowDocked();

            FlashbackConfig config = Flashback.getConfig();

            if (!config.signedRenderFilter) {
                String name = Minecraft.getInstance().getGameProfile().getName();
                ImGui.pushTextWrapPos(300);
                ImGui.textWrapped("我, " + name + ", 我郑重发誓，我不会在“渲染过滤器”菜单中关闭某些功能，然后在 Flashback 支持中询问为什么实体没有被渲染。（白话翻译：你不要在渲染过滤器把一些东西给关了，然后去问作者为什么xxx没有显示。因为作者被问烦了所以写了这一段话。）");
                if (ImGui.checkbox("Signed, " + name + ".", false)) {
                    config.signedRenderFilter = true;
                    config.delayedSaveToDefaultFolder();
                }
                ImGui.popTextWrapPos();
                ImGui.end();
                return;
            }

            EditorState editorState = EditorStateManager.getCurrent();

            if (editorState == null) {
                ImGui.end();
                return;
            }

            if (ImGui.beginTabBar("##Select")) {
                if (ImGui.beginTabItem("实体")) {
                    ImGui.inputText("搜索", entitySearch);
                    String searchString = ImGuiHelper.getString(entitySearch).trim().toLowerCase(Locale.ROOT);
                    if (!searchString.equals(lastEntitySearch)) {
                        lastEntitySearch = searchString;
                        searchedEntityTypes = new ArrayList<>();

                        List<EntityType<?>> contains = new ArrayList<>();

                        for (EntityType<?> entityType : BuiltInRegistries.ENTITY_TYPE) {
                            String name = I18n.get(entityType.getDescriptionId()).trim().toLowerCase(Locale.ROOT);
                            if (name.startsWith(lastEntitySearch)) {
                                searchedEntityTypes.add(entityType);
                            } else if (name.contains(lastEntitySearch)) {
                                contains.add(entityType);
                            }
                        }

                        searchedEntityTypes.addAll(contains);
                    }

                    if (searchedEntityTypes.isEmpty()) {
                        ImGui.text("未找到任何实体");
                    } else {
                        if (ImGui.beginChild("##Scroller", 0, 300)) {
                            ImGuiListClipper.forEach(searchedEntityTypes.size(), new ImListClipperCallback() {
                                @Override
                                public void accept(int i) {
                                    EntityType<?> entityType = searchedEntityTypes.get(i);
                                    ResourceLocation resourceLocation = BuiltInRegistries.ENTITY_TYPE.getKey(entityType);

                                    boolean filtered = editorState.filteredEntities.contains(resourceLocation.toString());

                                    String name = I18n.get(entityType.getDescriptionId());
                                    if (ImGui.checkbox(name + "###" + resourceLocation, !filtered)) {
                                        if (filtered) {
                                            editorState.filteredEntities.remove(resourceLocation.toString());
                                        } else {
                                            editorState.filteredEntities.add(resourceLocation.toString());
                                        }
                                    }
                                }
                            });
                        }
                        ImGui.endChild();

                        if (ImGui.smallButton("启动所有")) {
                            editorState.filteredEntities.clear();
                        }
                        ImGui.sameLine();
                        if (ImGui.smallButton("禁用所有")) {
                            for (EntityType<?> entityType : BuiltInRegistries.ENTITY_TYPE) {
                                ResourceLocation resourceLocation = BuiltInRegistries.ENTITY_TYPE.getKey(entityType);
                                editorState.filteredEntities.add(resourceLocation.toString());
                            }
                        }
                    }

                    ImGui.endTabItem();
                }
                if (ImGui.beginTabItem("粒子")) {
                    ImGui.inputText("搜索", particleSearch);
                    String searchString = ImGuiHelper.getString(particleSearch).trim().toLowerCase(Locale.ROOT);
                    if (!searchString.equals(lastParticleSearch)) {
                        lastParticleSearch = searchString;
                        searchedParticleTypes = new ArrayList<>();

                        List<ResourceLocation> contains = new ArrayList<>();

                        for (ParticleType<?> particleType : BuiltInRegistries.PARTICLE_TYPE) {
                            ResourceLocation resourceLocation = BuiltInRegistries.PARTICLE_TYPE.getKey(particleType);
                            if (resourceLocation == null) {
                                continue;
                            }

                            String name;
                            if (resourceLocation.getNamespace().equals("minecraft")) {
                                name = resourceLocation.getPath().toLowerCase(Locale.ROOT);
                            } else {
                                name = resourceLocation.toString().toLowerCase(Locale.ROOT);
                            }

                            if (name.startsWith(lastParticleSearch)) {
                                searchedParticleTypes.add(resourceLocation);
                            } else if (name.contains(lastParticleSearch)) {
                                contains.add(resourceLocation);
                            }
                        }

                        searchedParticleTypes.addAll(contains);
                    }

                    if (searchedParticleTypes.isEmpty()) {
                        ImGui.text("未找到任何粒子");
                    } else {
                        if (ImGui.beginChild("##Scroller", 0, 300)) {
                            ImGuiListClipper.forEach(searchedParticleTypes.size(), new ImListClipperCallback() {
                                @Override
                                public void accept(int i) {
                                    ResourceLocation particleType = searchedParticleTypes.get(i);

                                    boolean filtered = editorState.filteredParticles.contains(particleType.toString());

                                    String name;
                                    if (particleType.getNamespace().equals("minecraft")) {
                                        name = particleType.getPath().toLowerCase(Locale.ROOT);
                                    } else {
                                        name = particleType.toString().toLowerCase(Locale.ROOT);
                                    }

                                    if (ImGui.checkbox(name, !filtered)) {
                                        if (filtered) {
                                            editorState.filteredParticles.remove(particleType.toString());
                                        } else {
                                            editorState.filteredParticles.add(particleType.toString());
                                        }
                                    }
                                }
                            });
                        }
                        ImGui.endChild();

                        if (ImGui.smallButton("启用所有")) {
                            editorState.filteredParticles.clear();
                        }
                        ImGui.sameLine();
                        if (ImGui.smallButton("禁用所有")) {
                            for (ParticleType<?> particleType : BuiltInRegistries.PARTICLE_TYPE) {
                                ResourceLocation resourceLocation = BuiltInRegistries.PARTICLE_TYPE.getKey(particleType);
                                if (resourceLocation == null) {
                                    continue;
                                }
                                editorState.filteredParticles.add(resourceLocation.toString());
                            }
                        }
                    }

                    ImGui.pushTextWrapPos(300);
                    ImGui.textWrapped("注意：由于技术限制，只会阻止新粒子的产生。现有粒子不会受到影响。");
                    ImGui.popTextWrapPos();

                    ImGui.endTabItem();
                }
                ImGui.endTabBar();
            }
        }
        ImGui.end();
    }

}
