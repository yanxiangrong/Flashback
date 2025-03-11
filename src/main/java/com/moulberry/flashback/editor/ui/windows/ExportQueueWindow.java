package com.moulberry.flashback.editor.ui.windows;

import com.moulberry.flashback.Flashback;
import com.moulberry.flashback.editor.ui.ImGuiHelper;
import com.moulberry.flashback.exporting.ExportJob;
import com.moulberry.flashback.exporting.ExportJobQueue;
import com.moulberry.flashback.exporting.ExportSettings;
import imgui.ImGui;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiTableColumnFlags;
import imgui.flag.ImGuiTableFlags;
import imgui.flag.ImGuiWindowFlags;

public class ExportQueueWindow {

    private static boolean open = false;

    public static void open() {
        open = true;
    }

    public static void render() {
        if (open) {
            ImGui.openPopup("###ExportQueue");
            open = false;
        }

        if (ImGuiHelper.beginPopupModalCloseable("导出队列###ExportQueue", ImGuiWindowFlags.AlwaysAutoResize)) {
            ImGuiHelper.pushStyleColor(ImGuiCol.Border, 0xFF808080);

            boolean canStartJob = !ExportJobQueue.queuedJobs.isEmpty() && Flashback.EXPORT_JOB == null;
            boolean canRemoveJob = !ExportJobQueue.queuedJobs.isEmpty();

            ImGui.text("任务");
            if (ImGui.beginChild("##Jobs", 300, 150, true)) {
                if (ImGui.beginTable("##JobTable", 3, ImGuiTableFlags.SizingFixedFit)) {
                    ImGui.tableSetupColumn("任务名", ImGuiTableColumnFlags.WidthStretch);

                    int startJob = -1;
                    int removeJob = -1;

                    for (int i = 0; i < ExportJobQueue.queuedJobs.size(); i++) {
                        ExportSettings queuedJob = ExportJobQueue.queuedJobs.get(i);
                        String name = queuedJob.name() == null ? "任务 #" + (i+1) : queuedJob.name();

                        ImGui.tableNextColumn();
                        ImGui.text(name);
                        ImGui.tableNextColumn();
                        if (ImGui.smallButton("开始")) {
                            startJob = i;
                        }
                        ImGui.tableNextColumn();
                        if (ImGui.smallButton("移除")) {
                            removeJob = i;
                        }
                    }

                    if (startJob >= 0 && canStartJob) {
                        ExportSettings settings = ExportJobQueue.queuedJobs.remove(startJob);
                        Flashback.EXPORT_JOB = new ExportJob(settings);
                    } else if (removeJob >= 0 && canRemoveJob) {
                        ExportJobQueue.queuedJobs.remove(removeJob);
                    }

                    ImGui.endTable();
                }
                ImGui.endChild();
            }

            ImGuiHelper.popStyleColor();


            if (!canStartJob) ImGui.beginDisabled();
            if (ImGui.button("开始所有") && canStartJob) {
                ExportJobQueue.drainingQueue = true;
            }
            if (!canStartJob) ImGui.endDisabled();

            ImGui.sameLine();

            if (!canRemoveJob) ImGui.beginDisabled();
            if (ImGui.button("移除所有") && canRemoveJob) {
                ExportJobQueue.queuedJobs.clear();
            }
            if (!canRemoveJob) ImGui.endDisabled();

            ImGuiHelper.endPopupModalCloseable();
        }
    }

}
