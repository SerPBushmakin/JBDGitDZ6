// Реализация логгера
class Logger implements Observer {
    @Override
    // Просто выводит полученное сообщение в консоль с префиксом "LOG: ".
    public void update(String message) {
        System.out.println("LOG: " + message);
    }
}
