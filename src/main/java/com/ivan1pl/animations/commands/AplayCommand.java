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
package com.ivan1pl.animations.commands;

import com.ivan1pl.animations.constants.Messages;
import com.ivan1pl.animations.constants.Permissions;
import com.ivan1pl.animations.data.Animation;
import com.ivan1pl.animations.data.Animations;
import com.ivan1pl.animations.utils.MessageUtil;
import org.bukkit.command.CommandSender;

/**
 *
 * @author Ivan1pl
 */
public class AplayCommand extends AnimationsCommand {
    
    public AplayCommand() {
        super(Permissions.PERMISSION_USER, 1);
    }

    @Override
    protected void execute(CommandSender cs, String... args) {
        String name = args[0];
        Animation animation = Animations.getAnimation(name);
        if (animation != null) {
            animation.play();
        } else {
            MessageUtil.sendErrorMessage(cs, Messages.MSG_ANIMATION_NOT_FOUND, name);
        }
    }
    
}
