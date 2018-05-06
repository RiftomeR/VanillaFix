package org.dimdev.vanillafix;

import net.minecraft.client.gui.*;
import net.minecraft.client.resources.I18n;
import net.minecraft.crash.CrashReport;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dimdev.utils.HasteUpload;
import org.dimdev.utils.SSLUtils;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@SideOnly(Side.CLIENT)
public class GuiCrashScreen extends GuiScreen {
    private static final String HASTE_BASE_URL = "https://paste.dimdev.org";
    private static final Logger log = LogManager.getLogger();
    private static boolean patchedSSL = false;

    private File reportFile;
    private final CrashReport report;
    private String hasteLink = null;
    private Set<ModContainer> mods;
    private String modListString;

    public GuiCrashScreen(File reportFile, CrashReport report, Set<ModContainer> mods) {
        this.reportFile = reportFile;
        this.report = report;
        this.mods = mods;
    }

    @Override
    public void initGui() {
        buttonList.clear();
        buttonList.add(new GuiOptionButton(0, width / 2 - 155, height / 4 + 120 + 12, I18n.format("gui.toTitle")));
        buttonList.add(new GuiOptionButton(1, width / 2 - 155 + 160, height / 4 + 120 + 12, I18n.format("vanillafix.gui.getLink")));
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        try {
            if (button.id == 0) {
                mc.displayGuiScreen(new GuiMainMenu());
                ((IPatchedMinecraft) mc).clearCurrentReport();
            } else if (button.id == 1) {
                if (hasteLink == null) {
                    // This is just a quick fix for now. Instead, we should use a TrustManager that wraps the
                    // default one and trusts IdenTrust, and eventually removed when Minecraft updates to a
                    // version of Java that trusts IdenTrust (root certificate for Let's Encrypt).
                    // See: https://stackoverflow.com/questions/34110426/does-java-support-lets-encrypt-certificates
                    if (!patchedSSL) {
                        // TODO: Remove this as soon as possible, or at least restore old TrustManager after upload
                        SSLUtils.trustAllCertificates();
                        patchedSSL = true;
                    }

                    hasteLink = HasteUpload.uploadToHaste(HASTE_BASE_URL, "txt", report.getCompleteReport());
                }
                ReflectionHelper.findField(GuiScreen.class, "clickedLinkURI", "field_175286_t").set(this, new URI(hasteLink));
                mc.displayGuiScreen(new GuiConfirmOpenLink(this, hasteLink, 31102009, false));
            }
        } catch (Throwable e) {
            log.error("Exception when crash menu button clicked:", e);
            button.displayString = I18n.format("vanillafix.gui.failed");
            button.enabled = false;
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) {
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) { // TODO: localize number of lines
        drawDefaultBackground();
        drawCenteredString(fontRenderer, I18n.format("vanillafix.crashscreen.title"), width / 2, height / 4 - 40, 0xFFFFFF);

        int textColor = 0xD0D0D0;
        int x = width / 2 - 155;
        int y = height / 4;

        drawString(fontRenderer, I18n.format("vanillafix.crashscreen.summary"), x, y, textColor);
        drawString(fontRenderer, I18n.format("vanillafix.crashscreen.paragraph1.line1"), x, y += 18, textColor);


        drawCenteredString(fontRenderer, getModListString(), width / 2, y += 11, 0xE0E000);

        drawString(fontRenderer, I18n.format("vanillafix.crashscreen.paragraph2.line1"), x, y += 11, textColor);
        drawString(fontRenderer, I18n.format("vanillafix.crashscreen.paragraph2.line2"), x, y += 9, textColor);

        drawCenteredString(fontRenderer, reportFile != null ? "\u00A7n" + reportFile.getName() : I18n.format("vanillafix.crashscreen.reportSaveFailed"), width / 2, y += 11, 0x00FF00);

        drawString(fontRenderer, I18n.format("vanillafix.crashscreen.paragraph3.line1"), x, y += 12, textColor);
        drawString(fontRenderer, I18n.format("vanillafix.crashscreen.paragraph3.line2"), x, y += 9, textColor);
        drawString(fontRenderer, I18n.format("vanillafix.crashscreen.paragraph3.line3"), x, y += 9, textColor);
        drawString(fontRenderer, I18n.format("vanillafix.crashscreen.paragraph3.line4"), x, y + 9, textColor);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    public String getModListString() {
        if (modListString == null) {
            List<String> modNames = new ArrayList<>();
            for (ModContainer mod : mods) {
                modNames.add(mod.getName());
            }
            if (modNames.isEmpty()) {
                modListString = "Unknown";
            } else {
                modListString = StringUtils.join(modNames, ", ");
            }
        }
        return modListString;
    }
}
