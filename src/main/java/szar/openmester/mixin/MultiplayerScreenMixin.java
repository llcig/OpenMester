package szar.openmester.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerServerListWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.option.ServerList;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import szar.openmester.MesterMCAPI;
import szar.openmester.MesterMCScreen;

@Mixin(MultiplayerScreen.class)
public abstract class MultiplayerScreenMixin extends Screen {

    @Shadow protected MultiplayerServerListWidget serverListWidget;

    public MultiplayerScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("RETURN"))
    private void init(CallbackInfo ci) {
        this.addDrawableChild(ButtonWidget.builder(Text.of("MesterMC Login"), (button) ->
            MinecraftClient.getInstance().setScreen(new MesterMCScreen(((MultiplayerScreen)(Object)this)))
        ).width(100).position(5, 5).build());

        this.addDrawableChild(ButtonWidget.builder(Text.of("Add MesterMC Servers"), (button) -> {
            try {
                ServerList serverList = ((MultiplayerScreenAccessor)(Object)this).getServerList();
                for (MesterMCAPI.MesterMCServer server : MesterMCAPI.getServers().servers) {
                    serverList.add(new ServerInfo(server.name, server.ip + ":" + server.port, ServerInfo.ServerType.OTHER), false);
                }
                serverList.saveFile();
                this.serverListWidget.setServers(serverList);
                button.setMessage(Text.of("Done!"));
            } catch (Exception e) {
                e.printStackTrace();
                button.setMessage(Text.of("An error occurred"));
            }
        }).width(100).position(110, 5).build());
    }

}
