# Spring IoC & Dependency Injection — XML Exercises

These exercises are designed for practicing **Spring IoC and Dependency Injection using XML configuration**.

---

## Exercise 1 — Employee & Address

Create an `Employee` class with:

- `id`
- `name`
- `salary`
- `Address address`

Create an `Address` class with:

- `city`
- `state`
- `pincode`

Configure both beans using `applicationContext.xml`.

### Requirements

- Use **setter injection**.
- `Employee` should receive its `Address` object from Spring.
- Retrieve the `Employee` object from the Spring container and print all details.

### Concepts

- IoC
- Setter DI
- Bean properties
- Object dependency

---

## Exercise 2 — Constructor Injection

Create a `Car` class with:

- `model`
- `price`
- `Engine engine`

Create an `Engine` class with:

- `type`
- `horsePower`

Configure the `Engine` and inject it into `Car` using **constructor injection**.

### Expected Output

```text
Car Model: Honda City
Price: 1500000
Engine: Petrol
Horse Power: 120
```

### Concept

- Constructor-based DI

---

## Exercise 3 — Student and Course

Create:

```text
Student
 ├── id
 ├── name
 └── Course course

Course
 ├── courseId
 ├── courseName
 └── duration
```

Configure the objects using XML.

### Example Output

```text
Student: Rahul
Course: Java
Duration: 3 Months
```

### Requirements

- Use setter injection.
- `Student` should not create the `Course` object using `new`.
- Spring should inject the dependency.

### Concept

- Dependency inversion through Spring

---

## Exercise 4 — Bank Account + Customer

Create:

```text
Customer
 ├── customerId
 ├── name
 └── email

BankAccount
 ├── accountNumber
 ├── balance
 └── Customer customer
```

Configure both beans using XML.

### Expected Output

```text
Account Number: 10101
Balance: 50000
Customer ID: C101
Customer Name: Amit
Email: amit@gmail.com
```

### Requirements

- First implement using setter injection.
- Then try the same exercise using constructor injection.

### Concepts

- Setter injection
- Constructor injection
- Object-to-object dependency

---

## Exercise 5 — Interface-Based DI ⭐

Create an interface:

```java
public interface MessageService {
    void sendMessage(String message);
}
```

Create two implementations:

```text
EmailMessageService
SMSMessageService
```

Then create:

```text
NotificationService
 └── MessageService messageService
```

`NotificationService` should not know whether the message is sent through email or SMS.

### Requirements

1. Configure `EmailMessageService` in XML first.
2. Inject it into `NotificationService`.
3. Run the application.
4. Change the XML configuration so that `SMSMessageService` is injected instead.
5. Do **not** modify `NotificationService.java`.

### Concepts

- IoC
- DI
- Interface-based programming
- Loose coupling

---

## Exercise 6 — Product & Category

Create:

```text
Product
 ├── id
 ├── name
 ├── price
 └── Category category

Category
 ├── id
 └── name
```

### Example Output

```text
Product ID: 101
Product Name: Laptop
Price: 65000

Category ID: 10
Category Name: Electronics
```

Configure everything using `applicationContext.xml`.

### Challenge

Create **three different Product beans** that use the same Category bean.

For example:

```text
Laptop  → Electronics
Mobile  → Electronics
Tablet  → Electronics
```

Think about what happens when multiple products reference:

```xml
<bean id="category" ...>
```

### Concepts

- Bean reuse
- Dependency injection
- Spring bean scope

---

## Exercise 7 — Mini Project: Restaurant

Build a small Spring XML application.

Create:

```text
Restaurant
 ├── name
 ├── address
 └── chef

Chef
 ├── name
 └── speciality
```

Create two chefs:

```text
Chef 1 → Raj → Indian
Chef 2 → John → Italian
```

Create two restaurants and inject the appropriate chef into each restaurant.

### Requirements

Your XML configuration should decide which chef each restaurant receives.

The Java classes should contain **no**:

```java
new Chef()
```

or:

```java
new Address()
```

for dependencies.

### Concepts

- IoC
- Dependency injection
- Bean configuration
- Loose coupling

---

## Exercise 8 — Dependency Chain ⭐⭐

This is a good exercise before moving to Spring annotations.

Create:

```text
OrderService
      ↓
PaymentService
      ↓
PaymentGateway
```

The dependency chain should look like:

```text
OrderService
    |
    ---> PaymentService
              |
              ---> PaymentGateway
```

Create these classes:

```text
OrderService
PaymentService
PaymentGateway
```

Configure all three using XML.

When you call:

```java
orderService.placeOrder();
```

the output should be similar to:

```text
Placing order...
Processing payment...
Connecting to payment gateway...
Payment successful!
Order placed successfully!
```

### Goal

Understand how Spring can build an entire **dependency graph** automatically.

### Concepts

- Nested dependencies
- Dependency graphs
- IoC container
- XML-based DI

---

# Recommended Order

Practice the exercises in this sequence:

```text
1 → 2 → 3 → 4 → 5 → 6 → 8 → 7
```

If you can comfortably solve **Exercises 5, 6, and 8 without looking at examples**, you should have a solid understanding of:

- Spring IoC
- Dependency Injection
- Setter Injection
- Constructor Injection
- Interface-based DI
- Loose coupling
- Bean reuse
- Dependency graphs
- XML bean configuration

