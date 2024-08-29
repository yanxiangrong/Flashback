package com.moulberry.flashback.exporting;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

public class SaveableFramebufferQueue implements AutoCloseable {

    private final int width;
    private final int height;

    private static final int CAPACITY = 3;

    private final List<SaveableFramebuffer> available = new ArrayList<>();
    private final List<SaveableFramebuffer> waiting = new ArrayList<>();

    private final RenderTarget flipbuffer;
    private final ShaderInstance flipShader;

    public SaveableFramebufferQueue(int width, int height) {
        this.width = width;
        this.height = height;
        this.flipbuffer = new TextureTarget(width, height, false, false);

        for (int i = 0; i < CAPACITY; i++) {
            this.available.add(new SaveableFramebuffer());
        }

        try {
            this.flipShader = new ShaderInstance(Minecraft.getInstance().getResourceManager(), "blit_screen_flip", DefaultVertexFormat.BLIT_SCREEN);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public SaveableFramebuffer take() {
        if (this.available.isEmpty()) {
            throw new IllegalStateException("No textures available!");
        }
        return this.available.removeFirst();
    }

    private void blitFlip(RenderTarget src) {
        GlStateManager._colorMask(true, true, true, true);
        GlStateManager._disableDepthTest();
        GlStateManager._depthMask(false);
        GlStateManager._viewport(0, 0, src.width, src.height);
        GlStateManager._disableBlend();
        RenderSystem.disableCull();

        this.flipbuffer.bindWrite(true);
        this.flipShader.setSampler("DiffuseSampler", src.colorTextureId);
        this.flipShader.apply();
        BufferBuilder bufferBuilder = RenderSystem.renderThreadTesselator().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.BLIT_SCREEN);
        bufferBuilder.addVertex(0.0F, 1.0F, 0.0F);
        bufferBuilder.addVertex(1.0F, 1.0F, 0.0F);
        bufferBuilder.addVertex(1.0F, 0.0F, 0.0F);
        bufferBuilder.addVertex(0.0F, 0.0F, 0.0F);
        BufferUploader.draw(bufferBuilder.buildOrThrow());
        this.flipShader.clear();

        GlStateManager._depthMask(true);
        GlStateManager._colorMask(true, true, true, true);
        RenderSystem.enableCull();
    }

    public void startDownload(RenderTarget target, SaveableFramebuffer texture) {
        //Do an inline flip
        this.blitFlip(target);

        texture.startDownload(this.flipbuffer, this.width, this.height);
        this.waiting.add(texture);
    }

    record DownloadedFrame(NativeImage image, @Nullable FloatBuffer audioBuffer) {}

    public @Nullable DownloadedFrame finishDownload(boolean drain) {
        if (this.waiting.isEmpty()) {
            return null;
        }

        if (!drain && !this.available.isEmpty()) {
            return null;
        }

        SaveableFramebuffer texture = this.waiting.removeFirst();

        NativeImage nativeImage = texture.finishDownload(this.width, this.height);
        FloatBuffer audioBuffer = texture.audioBuffer;
        texture.audioBuffer = null;

        this.available.add(texture);
        return new DownloadedFrame(nativeImage, audioBuffer);
    }

    @Override
    public void close() {
        for (SaveableFramebuffer texture : this.waiting) {
            texture.close();
        }
        for (SaveableFramebuffer texture : this.available) {
            texture.close();
        }
        this.waiting.clear();
        this.available.clear();
        this.flipbuffer.destroyBuffers();
        this.flipShader.close();
    }


}
