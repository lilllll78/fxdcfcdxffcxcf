package fr.chestesp;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/** Liste les schemas du dossier .minecraft/schematics. */
public class SchematicListScreen extends Screen {

    private static final int PER_PAGE = 7;
    private final Screen parent;
    private List<Path> files = new ArrayList<>();
    private int page = 0;

    public SchematicListScreen(Screen parent) {
        super(Text.literal("Choisir un schéma"));
        this.parent = parent;
    }

    private static Path dir() {
        return FabricLoader.getInstance().getGameDir().resolve("schematics");
    }

    @Override
    protected void init() {
        files = new ArrayList<>();
        try {
            Files.createDirectories(dir());
            try (Stream<Path> s = Files.list(dir())) {
                s.filter(p -> {
                    String n = p.getFileName().toString().toLowerCase();
                    return n.endsWith(".litematic") || n.endsWith(".schem") || n.endsWith(".nbt");
                }).sorted().forEach(files::add);
            }
        } catch (IOException ignored) {
        }

        int w = 260, h = 20;
        int x = this.width / 2 - w / 2;
        int y = this.height / 2 - 75;
        int maxPage = Math.max(0, (files.size() - 1) / PER_PAGE);
        page = Math.min(page, maxPage);

        for (int i = 0; i < PER_PAGE; i++) {
            int idx = page * PER_PAGE + i;
            if (idx >= files.size()) break;
            Path f = files.get(idx);
            String name = f.getFileName().toString();
            String label = name.length() > 38 ? name.substring(0, 35) + "..." : name;
            this.addDrawableChild(ButtonWidget.builder(Text.literal(label), b -> load(f))
                    .dimensions(x, y + i * 22, w, h).build());
        }

        int by = y + PER_PAGE * 22 + 4;
        this.addDrawableChild(ButtonWidget.builder(Text.literal("<"), b -> { page = Math.max(0, page - 1); rebuild(); })
                .dimensions(x, by, 60, h).build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Retour"), b -> this.client.setScreen(parent))
                .dimensions(x + 70, by, 120, h).build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal(">"), b -> { page = Math.min(maxPage, page + 1); rebuild(); })
                .dimensions(x + 200, by, 60, h).build());
    }

    private void rebuild() {
        this.clearChildren();
        this.init();
    }

    private void load(Path f) {
        MinecraftClient mc = MinecraftClient.getInstance();
        try {
            Schematic s = Schematic.load(f);
            BuildState.setSchematic(s, f.getFileName().toString());
            if (BuildState.origin == null && mc.player != null) BuildState.origin = mc.player.getBlockPos();
            if (mc.player != null) {
                mc.player.sendMessage(Text.literal("Schéma chargé : " + f.getFileName() + " (" + s.sx + "x" + s.sy + "x" + s.sz + ")"), false);
            }
        } catch (Exception e) {
            if (mc.player != null) {
                mc.player.sendMessage(Text.literal("Erreur de chargement : " + e.getMessage()), false);
            }
        }
        mc.setScreen(parent);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, this.height / 2 - 100, 0xFFFFFF);
        if (files.isEmpty()) {
            context.drawCenteredTextWithShadow(this.textRenderer,
                    Text.literal("Aucun schéma. Mets des .litematic / .schem / .nbt dans :"),
                    this.width / 2, this.height / 2 - 60, 0xFFAAAA);
            context.drawCenteredTextWithShadow(this.textRenderer,
                    Text.literal(dir().toString()), this.width / 2, this.height / 2 - 46, 0xAAAAAA);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
