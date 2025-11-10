package moe.gensoukyo.nonapanel.api;

import net.minecraft.world.phys.Vec3;

public class SimpleVec3 {
    public final double x;
    public final double y;
    public final double z;

    public SimpleVec3(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public SimpleVec3(Vec3 vec3){
        this.x = vec3.x;
        this.y = vec3.y;
        this.z = vec3.z;
    }
}

