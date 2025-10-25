package plugin.oreMining.data;

import lombok.Getter;

/**
 * 鉱石のスコア情報を扱うオブジェクト
 * 鉱石の種類・点数の情報を持つ
 */
public class OreData {

  @Getter
  public enum OreType {
    COAL_ORE(10, "石炭"),
    IRON_ORE(20, "鉄"),
    COPPER_ORE(30, "銅"),
    GOLD_ORE(40, "金"),
    EMERALD_ORE(50, "エメラルド"),
    DIAMOND_ORE(60, "ダイヤモンド"),
    UNKNOWN(0, "");

    private final int orePoint;
    private final String oreName;

    OreType(int orePoint, String oreName) {
      this.orePoint = orePoint;
      this.oreName = oreName;
    }

    public static OreType fromBlockType(String blockType) {
      try {
        return OreType.valueOf(blockType);
      } catch (IllegalArgumentException e) {
        return UNKNOWN;
      }
    }
  }
}