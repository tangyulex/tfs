CREATE TABLE IF NOT EXISTS TM_STOCK
(
    STOCK_CODE             VARCHAR(8) COMMENT '股票代码',
    F1                     VARCHAR(64) COMMENT '"f1": 2',
    F2                     VARCHAR(64) COMMENT '最新价 "f2": 23.82',
    F3                     VARCHAR(64) COMMENT '涨跌幅% "f3": -3.09',
    F4                     VARCHAR(64) COMMENT '涨跌额 "f4": -0.76',
    F5                     VARCHAR(64) COMMENT '成交量(手) "f5": 1918867',
    F6                     VARCHAR(64) COMMENT '成交额 "f6": 4569742080.0',
    F7                     VARCHAR(64) COMMENT '振幅% "f7": 6.43',
    F8                     VARCHAR(64) COMMENT '换手率% "f8": 0.99',
    F9                     VARCHAR(64) COMMENT '市盈率(动态) "f9": 15.98',
    F10                    VARCHAR(64) COMMENT '量比 "f10": 1.54',
    F11                    VARCHAR(64) COMMENT '五分钟涨速 "f11": -0.21',
    F12                    VARCHAR(64) COMMENT '代码 "f12": "000001"',
    F13                    VARCHAR(64) COMMENT '"f13": 0',
    F14                    VARCHAR(64) COMMENT '名称 "f14": "平安银行"',
    F15                    VARCHAR(64) COMMENT '最高 "f15": 24.96',
    F16                    VARCHAR(64) COMMENT '最低 "f16": 23.38',
    F17                    VARCHAR(64) COMMENT '今开 "f17": 24.6',
    F18                    VARCHAR(64) COMMENT '昨收 "f18": 24.58',
    F20                    VARCHAR(64) COMMENT '总市值 "f20": 462248971476',
    F21                    VARCHAR(64) COMMENT '流通市值 "f21": 462245071595',
    F22                    VARCHAR(64) COMMENT '涨速 "f22": 0.04',
    F23                    VARCHAR(64) COMMENT '市净率 "f23": 1.57',
    F24                    VARCHAR(64) COMMENT '60日涨跌幅% "f24": 33.6',
    F25                    VARCHAR(64) COMMENT '年初至今涨跌幅 "f25": 23.16',
    F62                    VARCHAR(64) COMMENT '主力净流入额 "f62": -633123504.0',
    F115                   VARCHAR(64) COMMENT '市盈率(TTM) "f115": 15.98',
    CREATED_DATETIME       DATETIME NOT NULL COMMENT '创建时间',
    LAST_MODIFIED_DATETIME DATETIME NOT NULL COMMENT '修改时间',
    PRIMARY KEY (STOCK_CODE)
) COMMENT = '股票信息';


-- 日期,开盘,收盘,最高,最低,成交量,成交额,振幅%,涨跌幅%,涨跌额,换手率%
CREATE TABLE IF NOT EXISTS TM_STOCK_HISTORY
(
    STOCK_CODE             VARCHAR(8),
    STOCK_DATE             VARCHAR(64),
    NAME                   VARCHAR(64),
    KAI_PAN                VARCHAR(64),
    SHOU_PAN               VARCHAR(64),
    ZUI_GAO                VARCHAR(64),
    ZUI_DI                 VARCHAR(64),
    CHENG_JIAO_LIANG       VARCHAR(64),
    CHENG_JIAO_E           VARCHAR(64),
    ZHEN_FU                VARCHAR(64),
    ZHANG_DIE_FU           VARCHAR(64),
    ZHANG_DIE_E            VARCHAR(64),
    HUAN_SHOU_LV           VARCHAR(64),
    CREATED_DATETIME       DATETIME NOT NULL COMMENT '创建时间',
    LAST_MODIFIED_DATETIME DATETIME NOT NULL COMMENT '修改时间',
    PRIMARY KEY (STOCK_CODE, STOCK_DATE)
) COMMENT = '股票历史信息';