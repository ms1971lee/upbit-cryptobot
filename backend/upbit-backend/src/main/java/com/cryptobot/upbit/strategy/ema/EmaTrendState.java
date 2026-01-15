package com.cryptobot.upbit.strategy.ema;

/**
 * EMA 추세추종 전략 상태 머신
 * 상태 전이를 통해 무분별한 재진입과 중복 신호를 방지
 */
public enum EmaTrendState {
    
    /** 포지션 없음, 추세 대기 */
    FLAT("대기", "추세 확인 중"),
    
    /** 추세 확인됨, 눌림 대기 */
    WAIT_PULLBACK("눌림 대기", "추세 방향 확인, 눌림목 대기 중"),
    
    /** 눌림 감지됨, 트리거 대기 */
    WAIT_TRIGGER("트리거 대기", "눌림 감지, 진입 신호 대기 중"),
    
    /** 롱 포지션 보유 */
    IN_LONG("롱 진입", "롱 포지션 보유 중"),
    
    /** 숏 포지션 보유 */
    IN_SHORT("숏 진입", "숏 포지션 보유 중"),
    
    /** 쿨다운 (재진입 방지) */
    COOLDOWN("쿨다운", "재진입 방지 대기 중");
    
    private final String displayName;
    private final String description;
    
    EmaTrendState(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
}
