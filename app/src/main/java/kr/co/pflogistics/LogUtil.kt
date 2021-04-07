package kr.co.pflogistics

import android.util.Log

/**
 * 앱 전용 Log 유틸
 * @author hyobin im
 * @설명 '디버그 모드일 때만 실행'
 */

lateinit var classNm:String

class LogUtil(classStr: String) {
    
    init {
        classNm = classStr;
    }

    /* 로그 */
    fun i(msg: String) {
        if(BuildConfig.DEBUG){
            Log.i(classNm, "정보 \uD83D\uDC49\uD83D\uDC49" + buildLogMsg(msg))
        }
    }

    fun v(msg: String) {
        if(BuildConfig.DEBUG) {
            Log.v(classNm, "전체 \uD83D\uDC49\uD83D\uDC49" + buildLogMsg(msg))
        }
    }

    fun w(msg: String) {
        if(BuildConfig.DEBUG) {
            Log.w(classNm, "경고 \uD83D\uDC49\uD83D\uDC49" + buildLogMsg(msg))
        }
    }

    fun d(msg: String) {
        if(BuildConfig.DEBUG) {
            Log.d(classNm, "디버깅 \uD83D\uDC49\uD83D\uDC49" + buildLogMsg(msg))
        }
    }

    fun e(msg: String) {
        if(BuildConfig.DEBUG) {
            Log.e(classNm, "에러 \uD83D\uDC49\uD83D\uDC49" + buildLogMsg(msg))
        }
    }

    /**
     * 로그에 띄울 메세지를 만들어주는 메서드
     * @param logMsg
     * @return
     */

    private fun buildLogMsg(logMsg: String): String? {
        val ste = Thread.currentThread().stackTrace[4]
        val sb = StringBuilder()
        sb.append("  [")
        sb.append(ste.fileName.replace(".java", "::"))
        sb.append("]")
        sb.append("[")
        sb.append(ste.lineNumber)
        sb.append("]")
        sb.append("[")
        sb.append(ste.methodName)
        sb.append("] ")
        sb.append(logMsg)
        return sb.toString()
    }

}