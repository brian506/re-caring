package com.recaring.support.exception;

public enum ErrorCode {

    // Global
    E400, E401, E403, E404, E429, E500,

    // Auth (E2xxx) - JWT, OAuth, 로컬 인증, 비밀번호
    E2000, E2001, E2002, E2003, E2004, E2005, E2006, E2007, E2008,
    E2009, E2010, E2011, E2012, E2013, E2014, E2015, E2016, E2017, E2018,

    // Member (E3xxx) - 이메일, 계정
    E3000, E3001, E3002, E3003, E3004, E3005,

    // SMS / Phone Verification (E4xxx)
    E4000, E4001, E4002, E4003, E4004,

    // Care (E5xxx) - 케어 관계, 초대
    E5000, E5001, E5002, E5003, E5004, E5005, E5006, E5007, E5008, E5009, E5010,
    E5011, E5012,

    // Location (E6xxx) - GPS, SSE
    E6000, E6001, E6002,

    // Device (E7xxx) - Device Token
    E7000,

    // SafeZone (E8xxx) - 안심존
    E8000, E8001

}
