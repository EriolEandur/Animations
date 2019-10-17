/* 
 *  Copyright (C) 2016 Ivan1pl
 * 
 *  This file is part of Animations.
 * 
 *  Animations is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Animations is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Animations.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.ivan1pl.animations.data;

import com.ivan1pl.animations.AnimationsPlugin;
import com.ivan1pl.animations.constants.Messages;
import com.ivan1pl.animations.constants.OperationResult;
import com.ivan1pl.animations.tasks.AnimationTask;
import com.ivan1pl.animations.triggers.Trigger;
import com.ivan1pl.animations.triggers.TriggerBuilder;
import com.ivan1pl.animations.utils.MessageUtil;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

/**
 *
 * @author Ivan1pl, Eriol_Eandur
 */
public class Animations {
    
    private static final Map<String, Animation> animations = new HashMap<>();
    
    private static final Map<Animation, Trigger> triggers = new HashMap<>();
    
    private static final Map<UUID, Selection> selections = new HashMap<>();
    
    private static final Map<UUID, Location> blockSelections = new HashMap<>();
    
    private static final File PLUGIN_DIR = new File(AnimationsPlugin.getPluginInstance().getDataFolder()
            + File.separator + "animations");
    
    @Getter
    private static Material wandMaterial = null;
    
    @Getter
    private static Material blockSelectorMaterial = null;
    
    private static final Set<AnimationTask> runningTasks = new HashSet<>();
    
    private static final int PAGE_SIZE = 10;
    
    private static boolean debugMode = false;

    @Getter
    private static int editorTimeout;

    @Getter
    private static String editorEscapeString;
    
    @Getter
    private static int maxFrameSize = 0;
    
    private static int maxRunningAnimations = 0;
    
    private static int maxProcessedBlocks = 0;
    
    private static int currentSize = 0;
    
    static {
        if (!PLUGIN_DIR.exists()) {
            PLUGIN_DIR.mkdirs();
        }
    }
    
    private Animations() { }
    
    public static void reload() {
        debugMode = AnimationsPlugin.getPluginInstance().getConfig().getBoolean("debug.enabled");
        
        FilenameFilter aFilter = new FilenameFilter() {

            @Override
            public boolean accept(File file, String string) {
                return string.endsWith(".anim");
            }
            
        };
        FilenameFilter weAnimFilter = new FilenameFilter() {

            @Override
            public boolean accept(File file, String string) {
                return new File(file,string).isDirectory();
            }
            
        };
        
        String wand = AnimationsPlugin.getPluginInstance().getConfig().getString("wand");
        wandMaterial = Material.valueOf(wand);
        
        String blockSelectorWand = AnimationsPlugin.getPluginInstance().getConfig().getString("blockSelectorWand");
        blockSelectorMaterial = Material.valueOf(blockSelectorWand);

        editorTimeout = AnimationsPlugin.getPluginInstance().getConfig().getInt("editor.timeout");
        editorEscapeString = AnimationsPlugin.getPluginInstance().getConfig().getString("editor.escapeString");
        
        maxFrameSize = AnimationsPlugin.getPluginInstance().getConfig().getInt("limits.maxFrameSize");
        maxRunningAnimations = AnimationsPlugin.getPluginInstance().getConfig().getInt("limits.maxRunningAnimations");
        maxProcessedBlocks = AnimationsPlugin.getPluginInstance().getConfig().getInt("limits.maxProcessedBlocks");
        
        if (wandMaterial == null) {
            wandMaterial = Material.BLAZE_POWDER;
            AnimationsPlugin.getPluginInstance().getLogger().info(MessageUtil.formatMessage(Messages.INFO_INVALID_MATERIAL, wand, Material.BLAZE_POWDER.toString()));
        }
        
        if (blockSelectorMaterial == null) {
            blockSelectorMaterial = Material.BLAZE_ROD;
            AnimationsPlugin.getPluginInstance().getLogger().info(MessageUtil.formatMessage(Messages.INFO_INVALID_MATERIAL, blockSelectorWand, Material.BLAZE_ROD.toString()));
        }
        
        animations.clear();
        for (Trigger t : triggers.values()) {
            t.unregister();
        }
        triggers.clear();
        
        for (File f : PLUGIN_DIR.listFiles(weAnimFilter)) {
            String name = f.getName();
            
            reloadAnimation(name);
        }
        for (File f : PLUGIN_DIR.listFiles(aFilter)) {
            String name = f.getName();
            name = name.substring(0, name.length()-5);
            if(!animations.containsKey(name)) {
                reloadAnimation(name);
            }
        }
    }
    
    public static Animation getAnimation(String name) {
        return animations.get(name);
    }
    
    public static void setAnimation(String name, Animation animation) {
        animations.put(name, animation);
    }
    
    public static OperationResult saveAnimation(String name) {
        Animation animation = animations.get(name);
        if (animation != null) {
            if (!saveAnimation(name, animation)) {
                return OperationResult.INTERNAL_ERROR;
            }
        } else {
            return OperationResult.NOT_FOUND;
        }
        return OperationResult.SUCCESS;
    }
    
    private static boolean saveAnimation(String name, Animation animation) {
        FileOutputStream fstream = null;
        ObjectOutputStream ostream = null;
        boolean result = true;
        try {
            /*File folder = new File(PLUGIN_DIR, name);
            if(!folder.exists()) {
                folder.mkdir();
            }
            File f = new File(folder, "data.anim");
            Files.deleteIfExists(f.toPath());*/
            File f = new File(PLUGIN_DIR, name + ".anim");
            fstream = new FileOutputStream(f);
            ostream = new ObjectOutputStream(fstream);
            
            //ostream.writeObject(animation);
            animation.saveTo(f, ostream);
        } catch (IOException ex) {
            Logger.getLogger(Animations.class.getName()).log(Level.SEVERE, null, ex);
            result = false;
        } finally {
            try {
                if (ostream != null) {
                    ostream.close();
                }
                if (fstream != null) {
                    fstream.close();
                }
            } catch (IOException ex) {
                Logger.getLogger(Animations.class.getName()).log(Level.SEVERE, null, ex);
                result = false;
            }
        }
        return result;
    }
    
    public static boolean deleteAnimation(String name) {
        File f = new File(PLUGIN_DIR, name + ".anim");
        File folder = new File(PLUGIN_DIR, name);
        boolean retval = f.delete() || new File(folder,"data.anim").delete();
        for(File file: folder.listFiles()) {
            retval = retval && file.delete();
        }
        retval = retval && folder.delete();
        if (retval) {
            Animation animation = animations.get(name);
            if (animation != null) {
                Trigger t = triggers.get(animation);
                if (t != null) {
                    t.unregister();
                }
                triggers.remove(animation);
            }
            animations.remove(name);
        }
        return retval;
    }
    
    public static Selection getSelection(Player p) {
        if (p == null) {
            return null;
        }
        
        Selection s = selections.get(p.getUniqueId());
        if (s == null) {
            selections.put(p.getUniqueId(), new Selection());
            s = selections.get(p.getUniqueId());
        }
        
        return s;
    }
    
    public static Location getBlockSelection(Player p) {
        if (p == null) {
            return null;
        }
        
        Location l = blockSelections.get(p.getUniqueId());
        return l;
    }
    
    public static void setBlockSelection(Player p, Location selection) {
        blockSelections.put(p.getUniqueId(), selection);
    }
    
    public static void reloadAnimation(String name) {
        File f = new File(new File(PLUGIN_DIR, name),"data.anim");
        if(!f.exists()) {
            //conversion = true;
            f = new File(PLUGIN_DIR, name + ".anim");
        }
        FileInputStream fstream = null;
        ObjectInputStream ostream = null;
        try {
            fstream = new FileInputStream(f);
            ostream = new ObjectInputStream(fstream);

            Animation animation = (Animation) ostream.readObject();
            
            if (animation != null) {
                boolean conversion = animation.containsOutdatedFrame();
                if(conversion) {
                    Logger.getLogger(Animations.class.getName()).log(Level.INFO,"Converting old Animation: "+name);
                } else {
                    Logger.getLogger(Animations.class.getName()).log(Level.INFO,"Loading Animation: "+name);
                }
                try {
                    if(!animation.prepare(new File(PLUGIN_DIR, name))) {
                        Logger.getLogger(Animations.class.getName()).log(Level.WARNING,"Error while preparing Animation: "+name);
                        return;
                    }
                }catch(NullPointerException ex) {
                    File newFile = new File(f.getParent(),f.getName()+"_invalid");
                    f.renameTo(newFile);
                    Logger.getLogger(Animations.class.getName()).log(Level.WARNING,"Removed Animation: "
                                     +name+" as there was no matching world on the server."
                                     +"Data file was renamed to *_invalid");
                    return;
                }
                Animation oldAnimation = animations.get(name);
                if (oldAnimation != null) {
                    Trigger t = triggers.get(oldAnimation);
                    if (t != null) {
                        t.unregister();
                    }
                    triggers.remove(oldAnimation);
                }
                animations.put(name, animation);
                if (animation.getTriggerBuilderData() != null) {
                    Trigger t = new TriggerBuilder(animation)
                                    .setTriggerType(animation.getTriggerBuilderData().getType())
                                    .setRange(animation.getTriggerBuilderData().getRange())
                                    .setPassword(animation.getTriggerBuilderData().getPassword())
                                    .setTriggerBlocks(animation.getTriggerBuilderData().getTriggerBlocks())
                                    .setTriggerButtons(animation.getTriggerBuilderData().getTriggerButtons())
                                    .setAnimationName(animation.getTriggerBuilderData().getAnimationName())
                                    .setFrame(animation.getTriggerBuilderData().getFrame())
                                    .create();
                    t.register();
                    triggers.put(animation, t);
                }
                if(conversion) {
                    saveAnimation(name, animation);
                }
                AnimationsPlugin.getPluginInstance().getLogger().info(Messages.INFO_ANIMATION_LOADED + name);
            }
        } catch (IOException | ClassNotFoundException ex) {
            Logger.getLogger(Animations.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (ostream != null) {
                    ostream.close();
                }
                if (fstream != null) {
                    fstream.close();
                }
            } catch (IOException ex) {
                Logger.getLogger(Animations.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    public static boolean registerTask(AnimationTask task) {
        if ((runningTasks.size() < maxRunningAnimations || maxRunningAnimations == -1) &&
                (task.getAnimation().getSizeInBlocks() + currentSize <= maxProcessedBlocks || maxProcessedBlocks == -1)) {
            currentSize += task.getAnimation().getSizeInBlocks();
            runningTasks.add(task);
            return true;
        }
        return false;
    }
    
    public static AnimationTask retrieveTask(Animation animation) {
        for (AnimationTask task : runningTasks) {
            if (task.getAnimation() == animation) {
                return task;
            }
        }
        return null;
    }
    
    public static void deleteTask(AnimationTask task) {
        currentSize -= task.getAnimation().getSizeInBlocks();
        runningTasks.remove(task);
    }
    
    public static int countPages() {
        return Math.max(1, (animations.size() + PAGE_SIZE - 1)/PAGE_SIZE);
    }
    
    public static List<String> getPage(int page) {
        Set<String> animationSet = animations.keySet();
        List<String> list = new ArrayList<>(animationSet);
        Collections.sort(list);
        int from = (page - 1)*PAGE_SIZE;
        int to = page*PAGE_SIZE;
        if (from > list.size()) {
            from = list.size();
        }
        if (to > list.size()) {
            to = list.size();
        }
        return list.subList(from, to);
    }
    
    public static void debug(String message) {
        if (debugMode) {
            Logger.getLogger("DEBUG").log(Level.INFO, "[DEBUG] " + message);
        }
    }
    
    public static void debug(String message, Object... parameters) {
        debug(MessageFormat.format(message, parameters));
    }
    
    public static boolean validateSelectionSize(Selection s) {
        return s.getVolume() <= maxFrameSize || maxFrameSize == -1;
    }
    
    public static void callEvent(Event event) {
        Bukkit.getServer().getPluginManager().callEvent(event);
    }

    public static String[] getAnimationNames() {
        Set<String> names = animations.keySet();
        return names.toArray(new String[names.size()]);
    }
    
}
