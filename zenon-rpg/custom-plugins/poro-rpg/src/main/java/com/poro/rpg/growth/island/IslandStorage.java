package com.poro.rpg.growth.island;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class IslandStorage {
    private final Map<String, Long> itemCounts = new LinkedHashMap<>();

    public void add(String itemId, int qty) {
        add(itemId, (long) qty);
    }

    public void add(String itemId, long qty) {
        if (itemId == null || itemId.isBlank() || qty <= 0) {
            return;
        }
        itemCounts.merge(itemId.toUpperCase(), qty, Long::sum);
    }

    public void add(Material material, long qty) {
        if (material == null) {
            return;
        }
        add(material.name(), qty);
    }

    public long get(String itemId) {
        return itemCounts.getOrDefault(itemId == null ? "" : itemId.toUpperCase(), 0L);
    }

    public long getAmount(Material material) {
        return material == null ? 0L : get(material.name());
    }

    public long withdraw(String itemId, long qty) {
        if (itemId == null || itemId.isBlank() || qty <= 0) return 0L;
        String key = itemId.toUpperCase();
        long current = itemCounts.getOrDefault(key, 0L);
        long taken = Math.min(current, qty);
        if (taken <= 0) return 0L;
        long remaining = current - taken;
        if (remaining <= 0) itemCounts.remove(key);
        else itemCounts.put(key, remaining);
        return taken;
    }

    public long withdraw(Material material, long qty) {
        if (material == null || qty <= 0) {
            return 0L;
        }
        String key = material.name();
        long current = itemCounts.getOrDefault(key, 0L);
        long taken = Math.min(current, qty);
        if (taken <= 0) {
            return 0L;
        }
        long remaining = current - taken;
        if (remaining <= 0) {
            itemCounts.remove(key);
        } else {
            itemCounts.put(key, remaining);
        }
        return taken;
    }

    public Map<String, Long> getAll() {
        return Map.copyOf(itemCounts);
    }

    public void setAll(Map<String, Long> values) {
        itemCounts.clear();
        if (values == null) {
            return;
        }
        values.forEach((key, value) -> add(key, value == null ? 0L : value));
    }

    public List<Material> materialList() {
        List<Material> materials = new ArrayList<>();
        itemCounts.keySet().stream()
                .map(name -> Material.matchMaterial(name, false))
                .filter(java.util.Objects::nonNull)
                .sorted(Comparator.comparing(Material::name))
                .forEach(materials::add);
        return materials;
    }

    public int materialTypeCount() {
        return itemCounts.size();
    }

    public Map.Entry<Material, Long> topEntry() {
        return itemCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(entry -> {
                    Material material = Material.matchMaterial(entry.getKey(), false);
                    return material == null ? null : Map.entry(material, entry.getValue());
                })
                .orElse(null);
    }
}
