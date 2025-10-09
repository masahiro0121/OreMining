package plugin.oreMining.command;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.NotNull;
import plugin.oreMining.Main;
import plugin.oreMining.PlayerScoreData;
import plugin.oreMining.data.ExecutingPlayer;
import plugin.oreMining.mapper.PlayerScoreMapper;
import plugin.oreMining.mapper.data.PlayerScore;

/**
 * 制限時間内に鉱石ブロックを採掘し、スコアを獲得するゲームを起動するコマンド
 * スコアは鉱石によって変わり、採掘した鉱石の合計によってスコアが変動します
 * 結果はプレイヤー名、スコア、日時などで保存されます
 */
public class OreMiningCommand extends BaseCommand implements Listener {

  public static final int GAME_TIME = 20;
  public static final String LIST = "list";

  private final Main main;
  private final PlayerScoreData playerScoreData = new PlayerScoreData();
  private final List<ExecutingPlayer> executingPlayerList = new ArrayList<>();

  public OreMiningCommand(Main main) {
    this.main = main;
  }

  @Override
  public boolean onExecutePlayerCommand(Player player, @NotNull Command command,
      @NotNull String label, @NotNull String[] args) {
    //最初の引数が「list」だったらスコア情報一覧を表示して処理を終了する
    if(args.length == 1 && (LIST.equals(args[0]))){
      sendPlayerScoreList(player);
      return  false;
    }

    ExecutingPlayer nowExecutingPlayer = getPlayerScore(player);

    InitStatus(player);

    player.sendTitle("鉱石採掘ゲームスタート！",
        "ゲーム時間　残り" + nowExecutingPlayer.getGameTime() + "秒！",
        0, 60, 0);

    gamePlay(player, nowExecutingPlayer);
    return true;
  }


  @Override
  public boolean onExecuteNPCCommand(CommandSender sender) {
    return false;
  }

  /**
   * 現在登録されているスコアの一覧をメッセージに送る
   * @param player  プレイヤー
   */
  private void sendPlayerScoreList(Player player) {
    List<PlayerScore> playerScoreList = playerScoreData.selectList();

    for (PlayerScore playerScore : playerScoreList) {
      player.sendMessage(playerScore.getId() + " | "
          + playerScore.getPlayerName() + " | "
          + playerScore.getScore() + " | "
          + playerScore.getRegisteredAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
    }
  }

  @EventHandler
  public void onBlockBreak(BlockBreakEvent e) {
    // ここにブロックが壊された時の処理を追加
    Material blockType = e.getBlock().getType();
    Player player = e.getPlayer();

    if(Objects.isNull(player) || executingPlayerList.isEmpty()) {
      return;
    }

    executingPlayerList.stream()
        .filter(executingPlayer -> executingPlayer.getPlayerName().equals(player.getName()))
        .findFirst()
        .ifPresent(executingPlayer -> {
          if(!executingPlayer.isGameActive()) {
            return;
          }

          int point = switch (blockType) {
            case COAL_ORE -> 10;
            case IRON_ORE -> 20;
            case COPPER_ORE -> 30;
            case GOLD_ORE -> 40;
            case EMERALD_ORE -> 50;
            case DIAMOND_ORE -> 60;
            default -> 0;
          };

          String oreName = "";
          String string = switch (blockType) {
            case COAL_ORE -> oreName = "石炭";
            case IRON_ORE -> oreName = "鉄";
            case COPPER_ORE -> oreName = "銅";
            case GOLD_ORE -> oreName = "金";
            case EMERALD_ORE -> oreName = "エメラルド";
            case DIAMOND_ORE -> oreName = "ダイヤモンド";
            default -> oreName= "";
          };

          if(point != 0) {
            if(blockType.equals(executingPlayer.getLastBlockType())) {
              executingPlayer.setOreBonusCount(executingPlayer.getOreBonusCount() +1 );
            } else {
              executingPlayer.setOreBonusCount(1);
            }

            executingPlayer.setScore(executingPlayer.getScore() + point);
            player.sendMessage(
                oreName + "ブロックを壊した！現在のスコアは" + executingPlayer.getScore() + "点です！");

            if (executingPlayer.getOreBonusCount() >= 3 ) {
              executingPlayer.setScore(executingPlayer.getScore() + 30);
              player.sendMessage(
                  oreName + "ブロックを3連続で壊した！ボーナスポイント30点追加！");
            }

            executingPlayer.setLastBlockType(blockType);
          }

        });
  }

  /**
   * 現在コマンドを実行しているプレイヤーのスコア情報を取得する
   *
   * @param player コマンドを実行したプレイヤー
   * @return 現在コマンドを実行しているプレイヤーのスコア情報
   */
  private ExecutingPlayer getPlayerScore(Player player) {
    ExecutingPlayer executingPlayer = new ExecutingPlayer(player.getName());
    if(executingPlayerList.isEmpty()){
      executingPlayer = addNewPlayer(player);
    }else {
      executingPlayer = executingPlayerList.stream()
          .findFirst().map(ps -> ps.getPlayerName().equals(player.getName())
              ? ps
              : addNewPlayer(player)).orElse(executingPlayer);
    }

    executingPlayer.setGameTime(GAME_TIME);
    executingPlayer.setScore(0);
    return executingPlayer;
  }

  /**
   * ゲームを始める前にプレイヤーの状態を設定する。
   * 体力と空腹度を最大にして、メインハンドにネザライトのツルハシ、オフハンドに松明を64個持たせる。
   *
   * @param player　コマンドを実行したプレイヤー
   */
  private static void InitStatus(Player player) {
    //プレイヤーの状態を初期化する
    player.setHealth(20);
    player.setFoodLevel(20);

    PlayerInventory inventory = player.getInventory();
    inventory.setItemInMainHand(new ItemStack(Material.NETHERITE_PICKAXE));
    inventory.setItemInOffHand(new ItemStack(Material.TORCH,64));
  }

  /**
   * 新規のプレイヤー情報をリストに追加します
   *
   * @param player　コマンドを実行したプレイヤー
   * @return 新規プレイヤー
   */
  private ExecutingPlayer addNewPlayer(Player player) {
    ExecutingPlayer newPlayer = new ExecutingPlayer(player.getName());
    executingPlayerList.add(newPlayer);
    return newPlayer;
  }

  /**
   * ゲームを実行します。基底の時間内に敵を倒すとスコアが加算される。時間経過後に合計スコアを表示
   *
   * @param player コマンドを実行したプレイヤー
   * @param nowExecutingPlayer プレイヤースコア情報
   */
  private void gamePlay(Player player, ExecutingPlayer nowExecutingPlayer) {
    nowExecutingPlayer.setGameActive(true);
    Bukkit.getScheduler().runTaskTimer(main, Runnable -> {
      if(nowExecutingPlayer.getGameTime() <= 0) {
        Runnable.cancel();

        nowExecutingPlayer.setGameActive(false);

        player.sendTitle("ゲーム終了！",
            nowExecutingPlayer.getPlayerName() + "合計" + nowExecutingPlayer.getScore() + "点！",
            0, 60, 0);

        // スコア登録処理
        playerScoreData.insert(new PlayerScore(nowExecutingPlayer.getPlayerName(), nowExecutingPlayer.getScore()));

        return ;
      }
      nowExecutingPlayer.setGameTime(nowExecutingPlayer.getGameTime() - 1);
    },0, 1*20);
  }

}

