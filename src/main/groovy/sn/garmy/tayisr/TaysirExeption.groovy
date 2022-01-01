package sn.garmy.tayisr

class TaysirException extends RuntimeException {

    private static final long serialVersionUID = 8302460224741666076L

    String codeMsg
    String msgToLog
    Object obj1
    Object errors

    TaysirException() {
        super()
    }

    TaysirException(String message) {
        super(message)
    }

    TaysirException(Throwable e) {
        super(e)
    }

    TaysirException(String message, String codeMsg) {
        super(message)
        this.codeMsg = codeMsg
    }

    TaysirException(String message, String codeMsg, Object errors) {
        super(message)
        this.codeMsg = codeMsg
        this.errors = errors
    }

    TaysirException(String message, String codeMsg, String msgToLog) {
        super(message)
        this.codeMsg = codeMsg
        this.msgToLog = msgToLog
    }

    TaysirException(String message, String codeMsg, String msgToLog, Object obj1) {
        super(message)
        this.codeMsg = codeMsg
        this.msgToLog = msgToLog
        this.obj1 = obj1
    }

    String getMsgToLog() {
        if (msgToLog) {
            this.msgToLog
        } else {
            this.getMessage()
        }
    }

    String toString() {
        return  "status :error , code :  ${codeMsg} ,message :  ${message} ,obj1 :  ${obj1} ,msgToLog :  ${msgToLog} "
    }
}

