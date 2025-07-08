package szar.openmester.mixin;

import net.minecraft.client.session.Session;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.UUID;

@Mixin(Session.class)
public interface SessionAccessor {
    @Accessor("accessToken")
    void setAccessToken(String accessToken);

    @Accessor("uuid")
    void setUuid(UUID uuid);

    @Accessor("username")
    void setUsername(String username);
}
