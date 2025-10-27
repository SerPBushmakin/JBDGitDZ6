// Обмен валют
class CurrencyExchange extends Transaction {
    final String fromCurrency; // Валюта, которую меняют
    final String toCurrency; // Валюта, на которую меняют
    final double amount; // Сумма для обмена

    public CurrencyExchange(int clientId, String fromCurrency, String toCurrency, double amount) {
        super(clientId);
        this.fromCurrency = fromCurrency;
        this.toCurrency = toCurrency;
        this.amount = amount;
    }

    @Override
    public void process(Bank bank) {
        bank.exchangeCurrency(clientId, fromCurrency, toCurrency, amount);// Вызывает метод exchangeCurrency() объекта Bank
        bank.notifyObservers("Exchange: клиент #" + clientId + " обменял " + amount + " " + fromCurrency + " на " + toCurrency);// Уведомляет наблюдателей
    }
}
