package com.github.mrbest2525.evenIronGolemsRust;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.IronGolem;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.bukkit.Bukkit.getWorlds;

public final class EvenIronGolemsRust extends JavaPlugin implements Listener {
    
    private final Set<UUID> activeTasks = new HashSet<>();
    
    @Override
    public void onEnable() {
        // イベントリスナーの登録
        getServer().getPluginManager().registerEvents(this, this);
        
        // 起動時に既に存在するゴーレム全員にスケジュールを適用
        for (World world : getWorlds()) {
            for (IronGolem golem : world.getEntitiesByClass(IronGolem.class)) {
                startGolemLoop(golem);
            }
        }
        
        getLogger().info("Even Iron Golems rust - 腐食システムが稼働しました。");
    }
    
    @EventHandler
    public void onGolemSpawn(EntitySpawnEvent event) {
        if (event.getEntity() instanceof IronGolem ironGolem) {
            startGolemLoop(ironGolem);
        }
    }
    
    @EventHandler
    public void onEntitiesLoad(EntitiesLoadEvent event) {
        // ロードされた全エンティティをチェック
        for (Entity entity : event.getEntities()) {
            if (entity instanceof IronGolem golem) {
                startGolemLoop(golem);
            }
        }
    }
    
    @EventHandler
    public void onEntitiesUnload(org.bukkit.event.world.EntitiesUnloadEvent event) {
        for (Entity entity : event.getEntities()) {
            if (entity instanceof IronGolem) {
                activeTasks.remove(entity.getUniqueId());
            }
        }
    }
    
    /**
     * ゴーレム個体ごとに独立したタスクを割り当てる
     */
    public void startGolemLoop(IronGolem golem) {
        UUID uuid = golem.getUniqueId();
        
        // 既にリストにあれば二重登録しない
        if (activeTasks.contains(uuid)) return;
        
        activeTasks.add(uuid);
        
        golemLoop(golem);
    }
    
    public void golemLoop(IronGolem golem) {
        
        long delay = 1200L + (long) (Math.random() * (6000L - 1200L));
        
        golem.getScheduler().runDelayed(this, (task) -> {
            
            // エンティティが消滅、または死亡していたらタスクを終了
            if (!golem.isValid() || golem.isDead()) {
                task.cancel();
                activeTasks.remove(golem.getUniqueId());
                return;
            }
            
            // 「顔が水に浸かっているか」の判定
            if (golem.getEyeLocation().getBlock().getType() == Material.WATER) {
                
                // 2. 腐食ダメージ: ダメージ原因をDROWNING（溺死）に設定
                // 2.0 = ハート1個分。鉄が酸化して崩壊するイメージ。
                golem.damage(20.0, DamageSource.builder(DamageType.DROWN).build());
                
                // 3. 視覚演出: 腐食していることを示すパーティクル
                golem.getWorld().spawnParticle(
                        Particle.ASH,
                        golem.getEyeLocation(),
                        10, 0.5, 0.5, 0.5, 0.05
                );
            }
            golemLoop(golem);
        }, null, delay);
    }
}
