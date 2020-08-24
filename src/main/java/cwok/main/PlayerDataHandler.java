package cwok.main;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

// This class is going to do all of the work in writing to the file, finding when the player is done applying, sending the results to the admin, increasing the question number, and anything else.

public class PlayerDataHandler {

	FileConfiguration config = Greylist.plugin.getConfig();
	File datafolder = Greylist.plugin.getPluginDataFolder();
	
	@SuppressWarnings("unchecked")
	public List<String> getApplicationQuestions() {
		return (List<String>) config.getList("questions");
	}

	// Creates a list of players waiting for their
	// application to be checked
	public void CreateDataFiles() {
		File applicants = new File(datafolder + File.separator + "applicants.yml");
		if (!applicants.exists()) {
			try {
				applicants.createNewFile();
				FileConfiguration applicantsConfig = YamlConfiguration.loadConfiguration(applicants);
				applicantsConfig.createSection("applicants");
				applicantsConfig.save(applicants);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void AddPlayerToApplicants(String playername) {
		File applicants = new File(datafolder + File.separator + "applicants.yml");
		FileConfiguration applicantsConfig = YamlConfiguration.loadConfiguration(applicants);
		applicantsConfig.getConfigurationSection("applicants").set(playername.toLowerCase(), "true");

		try {
			applicantsConfig.save(applicants);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void RemovePlayerFromApplicants(String playername) {
		File applicants = new File(datafolder + File.separator + "applicants.yml");
		FileConfiguration applicantsConfig = YamlConfiguration.loadConfiguration(applicants);
		// Setting to null should remove the section... hopefully.
		applicantsConfig.getConfigurationSection("applicants").set(playername.toLowerCase(), "false");

		try {
			applicantsConfig.save(applicants);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public String GetPlayerToApplicants(String playername) {
		File applicants = new File(datafolder + File.separator + "applicants.yml");
		FileConfiguration applicantsConfig = YamlConfiguration.loadConfiguration(applicants);
		return applicantsConfig.getConfigurationSection("applicants").get(playername.toLowerCase()).toString();
	}

	public List<String> GetListOfApplicants() {
		File applicants = new File(datafolder + File.separator + "applicants.yml");
		FileConfiguration applicantsConfig = YamlConfiguration.loadConfiguration(applicants);
		List<String> applications = new ArrayList<String>();

		try {
			for (String key : applicantsConfig.getConfigurationSection("applicants").getKeys(false)) {
				//if (applicantsConfig.getConfigurationSection("applicants").getString(key) == "true") {
				if (applicantsConfig.getConfigurationSection("applicants").getString(key).length() == 4) {
					applications.add(key);
				}
			}
			return applications;
		} catch (Exception e) {
			return null;
		}
	}

	// Creates a user's data file
	public void CreateUserDataFile(OfflinePlayer op) {
		// Gets the player's file
		File playerFile = new File(
				datafolder + File.separator + "players" + File.separator + op.getUniqueId() + ".yml");
		// Checks if it doesn't exist
		if (!playerFile.exists()) {
			try {
				// Creates the file, loads the file, and sets the default values
				playerFile.createNewFile();
				FileConfiguration playerConfig = YamlConfiguration.loadConfiguration(playerFile);
				playerConfig.set("QuestionNumber", 0);
				playerConfig.set("Applying", false);
				playerConfig.set("FinishedApplying", false);
				playerConfig.createSection("QuestionAnswers");
				// Transfers the file from memory to disk
				// TODO: Check if any memory leaks occur from not unloading from memory
				playerConfig.save(playerFile);

				File applicants = new File(datafolder + File.separator + "applicants.yml");
				FileConfiguration applicantsConfig = YamlConfiguration.loadConfiguration(applicants);
				// Fix this.
				applicantsConfig.getConfigurationSection("applicants").set(op.getName().toLowerCase(), "false");

			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void DeleteUserDataFile(OfflinePlayer op) {
		// Gets the player's file
		File playerFile = new File(
				datafolder + File.separator + "players" + File.separator + op.getUniqueId() + ".yml");
		// Checks if it doesn't exist
		if (playerFile.exists()) {
			// Creates the file, loads the file, and sets the default values
			playerFile.delete();

			File applicants = new File(datafolder + File.separator + "applicants.yml");
			FileConfiguration applicantsConfig = YamlConfiguration.loadConfiguration(applicants);
			applicantsConfig.getConfigurationSection("applicants").set(op.getName().toLowerCase(), null);
		}
	}

	public boolean IsPlayerFinishedApplying(UUID uuid) {
		File playerFile = new File(datafolder + File.separator + "players" + File.separator + uuid + ".yml");
		FileConfiguration playerConfig = YamlConfiguration.loadConfiguration(playerFile);
		return playerConfig.getBoolean("FinishedApplying");
	}

	public boolean IsPlayerApplying(UUID uuid) {
		// Loads the player's file
		File playerFile = new File(datafolder + File.separator + "players" + File.separator + uuid + ".yml");
		FileConfiguration playerConfig = YamlConfiguration.loadConfiguration(playerFile);
		return playerConfig.getBoolean("Applying");
	}

	public void SetPlayerApplying(UUID uuid, boolean applying) {
		// Loads the player's file
		File playerFile = new File(datafolder + File.separator + "players" + File.separator + uuid + ".yml");
		FileConfiguration playerConfig = YamlConfiguration.loadConfiguration(playerFile);
		playerConfig.set("Applying", applying);
		try {
			playerConfig.save(playerFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void WriteQuestionAnswer(UUID UniqueID, String QuestionAnswer) {
		// Loads the player's file
		File playerFile = new File(datafolder + File.separator + "players" + File.separator + UniqueID + ".yml");
		FileConfiguration playerConfig = YamlConfiguration.loadConfiguration(playerFile);

		// Gets the player's question number
		int questionNumber = playerConfig.getInt("QuestionNumber");

		// Loads the part of the config where the player's answers are stored
		// Todo: Maybe create a section for the question?? That might fix it
		
		//playerConfig.createSection("QuestionAnswers");
		playerConfig.getConfigurationSection("QuestionAnswers").set(Integer.toString(questionNumber), QuestionAnswer);
		// Increases the question number
		questionNumber++;
		playerConfig.set("QuestionNumber", questionNumber);
		try {
			playerConfig.save(playerFile);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	// TODO: Combine the two methods below
	public void SetPlayerQuestionNumber(UUID UniqueID, int Number) {
		// Loads the player's file
		File playerFile = new File(datafolder + File.separator + "players" + File.separator + UniqueID + ".yml");
		FileConfiguration playerConfig = YamlConfiguration.loadConfiguration(playerFile);
		playerConfig.set("QuestionNumber", Number);
		try {
			playerConfig.save(playerFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public int GetPlayerQuestionNumber(UUID UniqueID) {
		// Loads the player's file
		File playerFile = new File(datafolder + File.separator + "players" + File.separator + UniqueID + ".yml");
		FileConfiguration playerConfig = YamlConfiguration.loadConfiguration(playerFile);
		// Returns Question Number
		return playerConfig.getInt("QuestionNumber");
	}

	public void FinishedApplication(UUID UniqueID) {
		// Loads the player's file
		File playerFile = new File(datafolder + File.separator + "players" + File.separator + UniqueID + ".yml");
		FileConfiguration playerConfig = YamlConfiguration.loadConfiguration(playerFile);
		playerConfig.set("QuestionNumber", 0);
		playerConfig.set("Applying", false);
		playerConfig.set("FinishedApplying", true);
		this.SetPlayerApplying(UniqueID, true);

		try {
			playerConfig.save(playerFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
		Player player = Bukkit.getPlayer(UniqueID);
		playerConfig.getConfigurationSection("QuestionAnswers").get("0");

		// Adds the player to the string of applicants config
		AddPlayerToApplicants(player.getName());

		Bukkit.getScheduler().runTask(Greylist.plugin, () -> { // Commands must be on the main thread
			for (Player playerIterator : Bukkit.getOnlinePlayers()) {
				if (playerIterator.hasPermission("greylist.recruiter")) {
					playerIterator.chat("/greylist info " + player.getName());
				}
			}
		});
	}
}