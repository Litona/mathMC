package de.litona.mathMC;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.mariuszgromada.math.mxparser.Expression;
import org.mariuszgromada.math.mxparser.Function;

import java.util.HashMap;
import java.util.Map;

public final class Main extends JavaPlugin {

	private static final String
		syntax =
		"§bmathMC §7>> §cSyntax: /plot (math function) [radius] | /plot undo | /plot save | /plot material (Bukkit-Material)\nYou can use these variables: x, z\nExample: /plot sqrt(x^2+z^2)";
	private static final Map<Block, Number[]> blocks = new HashMap<>();
	private static Material plotMaterial = Material.IRON_BLOCK;
	private static short a;

	@Override
	public void onEnable() {
		this.getCommand("plot").setExecutor(((sender, command, label, args) -> {
			if(sender instanceof Player) {
				Player p = (Player) sender;
				if(args.length != 0 && args.length < 3) {
					a = 100;
					if(args.length == 1) {
						if(args[0].equalsIgnoreCase("undo"))
							return undo(p);
						else if(args[0].equalsIgnoreCase("save")) {
							blocks.clear();
							p.sendMessage("§bmathMC §7>> §esaved");
							return true;
						}
					} else if(args.length == 2) {
						if(args[0].equalsIgnoreCase("material")) {
							if((plotMaterial = Material.valueOf(args[1].toUpperCase())) == null) {
								p.sendMessage("§bmathMC §7>> §cThe material you entered wasn't found. Changed back to default: IRON_BLOCK");
								plotMaterial = Material.IRON_BLOCK;
							} else
								p.sendMessage("§bmathMC §7>> §eThe plot material has been set to " + plotMaterial.toString());
							return true;
						} else
							try {
								a = Short.parseShort(args[1]);
								if(a % 2 == 1)
									p.sendMessage("§bmathMC §7>> §eYou entered an odd number. It has been fixed to §l" + ++a);
							} catch(NumberFormatException e) {
								p.sendMessage(syntax);
								return true;
							}
					}
					if(!blocks.isEmpty())
						undo(p);
					Block center = p.getWorld().getHighestBlockAt(p.getLocation());
					Function f = new Function("f(x,z)=" + args[0]);
					Bukkit.getScheduler().runTask(this, () -> {
						byte last = -1;
						for(short x = (short) -(a / 2); x <= a / 2; x++)
							for(short z = (short) -(a / 2); z <= a / 2; z++) {
								Block found = center.getWorld().getHighestBlockAt(x + center.getX(), z + center.getZ());
								Block calculated = found.getRelative(0, (int) new Expression("f(" + x + "," + z + ")", f).calculate(), 0);
								byte dif = (byte) (calculated.getY() - found.getY());
								for(byte ydif = dif; ydif != 0; ydif += (dif > 0 ? -1 : 1)) {
									calculated = found.getRelative(0, ydif, 0);
									if(!blocks.containsKey(calculated))
										blocks.put(calculated, new Number[] {calculated.getTypeId(), calculated.getData()});
									calculated.setType(dif > 0 ? plotMaterial : Material.AIR);
								}
								calculated = found.getRelative(0, 0, 0);
								if(!blocks.containsKey(calculated))
									blocks.put(calculated, new Number[] {calculated.getTypeId(), calculated.getData()});
								calculated.setType(dif > 0 ? plotMaterial : Material.AIR);

								dif = (byte) Math.floor(((x + a / 2) * a + z) * 100 / (a * a));
								if(dif % 20 == 0 && dif < 101 && dif > last)
									p.sendMessage("§bmathMC §7>> §edone: §l" + (last = dif) + "%");
							}
					});
				} else
					p.sendMessage(syntax);
			} else
				sender.sendMessage("§cThis command is for players only!");
			return true;
		}));
	}

	private static boolean undo(Player p) {
		blocks.forEach((b, n) -> b.setTypeIdAndData((int) n[0], (byte) n[1], false));
		blocks.clear();
		p.sendMessage("The last plot has been removed.");
		return true;
	}
}