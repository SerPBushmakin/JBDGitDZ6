// Реализация логгера
class Logger implements Observer {
    @Override
    public void update(String message) {
        System.out.println("LOG: " + message);
    }
}
