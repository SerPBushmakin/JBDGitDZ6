// Перевод между клиентами
class FundsTransfer extends Transaction {
    final int receiverId; // ID клиента-получателя
    final double amount; // Сумма перевода

    public FundsTransfer(int senderId, int receiverId, double amount) {
        super(senderId); // senderId - это clientId в базовом классе
        this.receiverId = receiverId;
        this.amount = amount;
    }

    @Override
    public void process(Bank bank) {
        bank.transferFunds(this.clientId, receiverId, amount); // Вызывает метод transferFunds() объекта Bank
        bank.notifyObservers("Transfer: клиент #" + clientId + " перевел " + amount + " клиенту #" + receiverId); // Уведомляет наблюдателей
    }
}
