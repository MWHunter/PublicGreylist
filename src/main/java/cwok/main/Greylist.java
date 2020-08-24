package cwok.main;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.List;

public class Greylist extends JavaPlugin implements Listener {

    public static Greylist plugin;

    public void onEnable() {

        plugin = this;

        // TODO: Make this into a config so everything isn't hard coded
        // Registers events
        getServer().getPluginManager().registerEvents(this, this);

        // Creates the config

        plugin.saveDefaultConfig();

        // Creates the player config folder
        File folder = new File(getDataFolder() + File.separator + "players");
        if (!folder.exists())
            folder.mkdirs();
        // Creates applicants.yml and greylist.yml
        PlayerDataHandler datahandler = new PlayerDataHandler();
        datahandler.CreateDataFiles();

        for (Player player : Bukkit.getOnlinePlayers()) {
            datahandler.CreateUserDataFile(player);
        }
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        PlayerDataHandler datahandler = new PlayerDataHandler();

        if (command.getName().equalsIgnoreCase("apply")) {

            if (sender instanceof Player) {
                Player player = (Player) sender;
                if (datahandler.IsPlayerFinishedApplying((player.getUniqueId()))) {
                    player.sendMessage(ChatColor.GREEN + "You have already applied for the greylist");
                } else {
                    datahandler.SetPlayerApplying(player.getUniqueId(), true);
                    datahandler.SetPlayerQuestionNumber(player.getUniqueId(), 0);

                    sender.sendMessage(ChatColor.AQUA
                            + "You are now in the in-game application process.  To go back to the last question, type "
                            + ChatColor.WHITE + "back" + ChatColor.AQUA + ", to exit this in-game application, type "
                            + ChatColor.WHITE + "exit" + ChatColor.AQUA + ".");

                    sender.sendMessage(ChatColor.GREEN + datahandler.getApplicationQuestions().get(0));
                }
            }
            return true;
        }

        if (command.getName().equalsIgnoreCase("greylist")) {

            // Checks permission before anything is done
            if (!sender.hasPermission("greylist.recruiter")) {
                return false;
            }

            if (args[0].equalsIgnoreCase("open")) { // Is the single argument that does not require a valid player
                List<String> applicants = datahandler.GetListOfApplicants();
                StringBuilder messageForJoiner = new StringBuilder();
                int iterator = 0;
                sender.sendMessage(ChatColor.AQUA + "List of open applications:");
                try {
                    for (String applier : applicants) {
                        messageForJoiner.append(applier);
                        messageForJoiner.append(" ");
                        iterator++;
                        // Put messages together to stop putting every word
                        // on
                        // separate
                        // lines
						//
						// comment above is kind of creative
						// TODO: Either cut this off at the max length of a message or evenly space names
                        if (iterator == 5) {
                            sender.sendMessage(messageForJoiner.toString());
                            messageForJoiner = new StringBuilder();
                            iterator = 0;
                        }
                    }
                } catch (Exception e) {
                    // Do nothing
                }
                // Sends any remaining messages
                if (iterator != 0) {
                    sender.sendMessage(messageForJoiner.toString());
                }
                return true;
            }

			// Checks if there is two arguments
			if (args.length != 2) {
				sender.sendMessage(ChatColor.AQUA + "Usage> " + ChatColor.DARK_AQUA
						+ "Greylist [Accept | Deny | Info | Open | Reset | Clear] [Player]");
				return true;
			}

			if (args[0].equalsIgnoreCase("info")) {

                // Loads the requested players configuration
                File dataFolder = Greylist.plugin.getPluginDataFolder();

                // Gets the players UUID
                OfflinePlayer op = Bukkit.getOfflinePlayer(args[1]);
                File playerFile = new File(dataFolder + File.separator + "players" + File.separator + op.getUniqueId() + ".yml");
                FileConfiguration playerConfig = YamlConfiguration.loadConfiguration(playerFile);

                if (datahandler.IsPlayerApplying(op.getUniqueId())) {
                    sender.sendMessage(ChatColor.DARK_AQUA + "This player has not finished their application!");
                    return true;
                }

                sender.sendMessage(ChatColor.DARK_AQUA + "Applicant: " + ChatColor.AQUA + args[1]);
                for (int i = 0; i < datahandler.getApplicationQuestions().size(); i++) {
                    sender.sendMessage(ChatColor.DARK_AQUA + datahandler.getApplicationQuestions().get(i) + " " + ChatColor.AQUA +
                            playerConfig.getConfigurationSection("QuestionAnswers").get(i + ""));
                }

                return true;
            }

            if (args[0].equalsIgnoreCase("accept")) {
                if (datahandler.GetPlayerToApplicants(args[1]).equalsIgnoreCase("true")) {
					for (String string : getConfig().getStringList("commandsRanOnAccept")) {
						Bukkit.getServer().dispatchCommand(getServer().getConsoleSender(), string.replace("{player}", args[1]));
					}

                    Player playerChosen = Bukkit.getServer().getPlayer(args[1]);

                    datahandler.RemovePlayerFromApplicants(args[1]);

                    try {
                        Bukkit.getServer().broadcastMessage(ChatColor.GREEN + playerChosen.getName()
                                + "'s application has been accepted by " + sender.getName());
                    } catch (Exception e) {
                        Bukkit.getServer().broadcastMessage(
                                ChatColor.GREEN + args[1] + "'s application has been accepted by " + sender.getName());
                    }
                } else {
                    sender.sendMessage(ChatColor.RED + "This player has already been accepted or denied");
                }

                return true;
            }

            if (args[0].equalsIgnoreCase("deny")) {
                if (datahandler.GetPlayerToApplicants(args[1]).equalsIgnoreCase("true")) {
                    Bukkit.getServer().broadcastMessage(
                            ChatColor.WHITE + args[1] + "'s " + "application has been denied by an admin");
					for (String string : getConfig().getStringList("commandsRanOnDeny")) {
						Bukkit.getServer().dispatchCommand(getServer().getConsoleSender(), string.replace("{player}", args[1]));
					}

                    datahandler.RemovePlayerFromApplicants(args[1]);

                } else {
                    sender.sendMessage(ChatColor.AQUA + "Usage> " + ChatColor.DARK_AQUA
                            + "Greylist [Accept | Deny | Info | Open | Reset | Clear] [Player]");
                }
                return true;
            }

            if (args[0].equalsIgnoreCase("reset")) {
                // Is offline just in case, works for online players too
                OfflinePlayer op = Bukkit.getOfflinePlayer(args[1]);
                // Recreates user's data files
                datahandler.DeleteUserDataFile(op);
                datahandler.CreateUserDataFile(op);
                datahandler.SetPlayerApplying(op.getUniqueId(), false);

                datahandler.RemovePlayerFromApplicants(args[1]);

                for (String string : getConfig().getStringList("commandsRanOnReset")) {
					Bukkit.getServer().dispatchCommand(getServer().getConsoleSender(), string.replace("{player}", args[1]));
				}

                sender.sendMessage(ChatColor.GREEN + args[1] + "'s files have been reset");

                return true;
            }

            if (args[0].equalsIgnoreCase("clear")) {
                datahandler.RemovePlayerFromApplicants(args[1]);
                return true;
            }

            sender.sendMessage(ChatColor.AQUA + "Usage> " + ChatColor.DARK_AQUA
                    + "Greylist [Accept | Deny | Info | Open | Reset | Clear] [Player]");
            return true;

        }
        return false;
    }

    // Here begins the simple methods that simply return simple things simply.
    // Only one method now, used to be a lot more.
	// second refactor comment.  the comment on the line two above this one isn't creative
	// okay the comment above isn't necessary
	// Do you think that comment was necessary?
	// stop it!
    public File getPluginDataFolder() {
        return getDataFolder();
    }

    // TODO: Clean up the commands and events into different classes

    // Everything below this line is an event
	// second refactor - the line above is true

    // Captures responses from chat
    @EventHandler
    public void onAsyncChatEvent(AsyncPlayerChatEvent event) {
        PlayerDataHandler datahandler = new PlayerDataHandler();
        Player chatter = event.getPlayer();
        boolean isPlayerApplying = datahandler.IsPlayerApplying(chatter.getUniqueId());
        if (isPlayerApplying) {
            String message = event.getMessage();

            int QuestionNumber = datahandler.GetPlayerQuestionNumber(chatter.getUniqueId());

            if (message.equalsIgnoreCase("back") || (message.equalsIgnoreCase("exit"))) {
                if (message.equalsIgnoreCase("back")) {
                    if (QuestionNumber != 0) {
                        datahandler.SetPlayerQuestionNumber(chatter.getUniqueId(),
                                datahandler.GetPlayerQuestionNumber(chatter.getUniqueId()) - 1);
                        chatter.sendMessage(
                                ChatColor.AQUA + "You have moved back to question number " + (QuestionNumber + 1));
                        chatter.sendMessage(datahandler.getApplicationQuestions().get(QuestionNumber));
                    } else {
                        chatter.sendMessage(ChatColor.AQUA + "You are already on the first Question!");
                    }
                }
                if (message.equalsIgnoreCase("exit")) {
                    datahandler.SetPlayerApplying(chatter.getUniqueId(), false);
                    datahandler.SetPlayerQuestionNumber(chatter.getUniqueId(), 0);
                    chatter.sendMessage(
                            ChatColor.RED + "You have quit the application process.  Please use /apply to apply again");
                }
                event.setCancelled(true);
            } else {
                if (datahandler.GetPlayerQuestionNumber(chatter.getUniqueId()) == datahandler.getApplicationQuestions().size() - 1) {
                    datahandler.WriteQuestionAnswer(chatter.getUniqueId(), event.getMessage());
                    datahandler.FinishedApplication(chatter.getUniqueId());

                    chatter.sendMessage(ChatColor.GREEN + "Your application has been sent.");
                    event.setCancelled(true);
                } else {
                    datahandler.WriteQuestionAnswer(chatter.getUniqueId(), event.getMessage());
                    chatter.sendMessage(ChatColor.GREEN
                            + datahandler.getApplicationQuestions().get(datahandler.GetPlayerQuestionNumber(chatter.getUniqueId())));
                    event.setCancelled(true);
                }
            }
        }
    }

    // Gives unregistered players god mode
    @EventHandler
    public void onEntityDamageEvent(EntityDamageEvent event) {
        Entity entity = event.getEntity();
        if (((entity instanceof Player)) && (!entity.hasPermission("greylist.accepted"))) {
            event.setCancelled(true);
        }
    }

    // Prevents unregistered players from PvP or PvE
    @EventHandler
    public void onEntityDamageByEntityEvent(EntityDamageByEntityEvent event) {
        Entity entity = event.getDamager();
        if (((entity instanceof Player)) && (!entity.hasPermission("greylist.accepted"))) {
            entity.sendMessage(ChatColor.AQUA + "Please /apply to become a member of this server");
            event.setCancelled(true);
        }
    }

    // Prevents unregistered players from starving
    @EventHandler
    public void onFoodLevelChangeEvent(FoodLevelChangeEvent event) {
        Entity entity = event.getEntity();
        if (((entity instanceof Player)) && (!entity.hasPermission("greylist.accepted"))) {
            event.setCancelled(true);
        }
    }

    // Prevents unregistered players from pressing buttons or activating pressure
    // plates
    @EventHandler
    public void onPlayerInteractEvent(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (!player.hasPermission("greylist.accepted") && event.getHand() == EquipmentSlot.HAND && (event.getItem() == null || !event.getItem().getType().equals(Material.WRITTEN_BOOK))) {
            player.sendMessage(ChatColor.AQUA + "Please /apply to become a member of this server");
            event.setCancelled(true);
        }
    }

    // Prevents unregistered players from picking up dropped items
    @EventHandler
    public void onEntityPickupItemEvent(EntityPickupItemEvent event) {
        Entity entity = event.getEntity();
        if (((entity instanceof Player)) && (!entity.hasPermission("greylist.accepted"))) {
            event.setCancelled(true);
        }
    }

    // Prevents unregistered players from placing blocks
    @EventHandler
    public void onBlockPlaceEvent(BlockPlaceEvent event) {
        Player blockplacer = event.getPlayer();
        if (!blockplacer.hasPermission("greylist.accepted")) {
            blockplacer.sendMessage(ChatColor.AQUA + "Please /apply to become a member of this server");
            event.setCancelled(true);
        }
    }

    // Prevents unregistered players from breaking blocks
    @EventHandler
    public void onBlockBreakEvent(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPermission("greylist.accepted")) {
            player.sendMessage(ChatColor.AQUA + "Please /apply to become a member of this server");
            event.setCancelled(true);
        }
    }

    // Prevents monsters from targeting unregistered players
    @EventHandler
    public void onEntityTargetEvent(EntityTargetEvent event) {
        Entity player = event.getTarget();
        if (((player instanceof Player)) && (!player.hasPermission("greylist.accepted"))) {
            event.setCancelled(true);
        }
    }

    // Prevents riding horses, interacting with villagers, and stuff
    @EventHandler
    public void onAnimalInteractEvent(PlayerInteractEntityEvent event) {
        Entity player = event.getPlayer();
        if (!player.hasPermission("greylist.accepted")) {
            player.sendMessage(ChatColor.AQUA + "Please /apply to become a member of this server");
            event.setCancelled(true);
        }
    }

    // Sends first time join messages and recruiters messages that the player who
    // joined is not registered
    @EventHandler
    public void onPlayerJoinEvent(PlayerJoinEvent event) {
        Player joiner = event.getPlayer();

        if (!joiner.hasPermission("greylist.accepted")) {
            Bukkit.getServer().broadcast(ChatColor.RED + joiner.getName() + ChatColor.DARK_AQUA + " is not greylisted!",
                    "greylist.recruiter");
        }
        PlayerDataHandler datahandler = new PlayerDataHandler();
        datahandler.CreateUserDataFile(joiner);

        // Sends recruiter names of all applicants
        // TODO: send players previous application questions, reset application
        // status of player
        if (joiner.hasPermission("greylist.recruiter")) {
            List<String> applicants = datahandler.GetListOfApplicants();
            StringBuilder messageForJoiner = new StringBuilder();
            int iterator = 0;
            joiner.sendMessage(ChatColor.AQUA + "List of open applications:");
            for (String applier : applicants) {
                messageForJoiner.append(applier);
                messageForJoiner.append(" ");
                iterator++;
                // Put messages together to stop putting every word
                // on
                // separate
                // lines
                if (iterator == 5) {
                    joiner.sendMessage(messageForJoiner.toString());
                    messageForJoiner = new StringBuilder();
                    iterator = 0;
                }
            }
            // Sends any remaining messages
            if (iterator != 0) {
                joiner.sendMessage(messageForJoiner.toString());
            }
        }
    }

    // why didn't you believe me?
}