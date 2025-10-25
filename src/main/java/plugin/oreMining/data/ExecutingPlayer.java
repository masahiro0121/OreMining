package plugin.oreMining.data;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Material;

/**
 * OreMiningのコマンドを実行する際のプレイヤー情報を扱うオブジェクト
 * プレイヤー名・スコア点数・日時などの情報を持つ
 */
@Getter
@Setter
public class ExecutingPlayer {
  private String playerName;
  private int score;
  private int gameTime;
  private boolean gameActive;
  private int oreBonusCount;
  private Material lastBlockType;

  public ExecutingPlayer(String playerName) {
    this.playerName = playerName;
    this.oreBonusCount = 1;
    this.lastBlockType = null;
  }
}
