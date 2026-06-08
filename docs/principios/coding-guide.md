# Principios

Este documento resume los principios y patrones vistos en las diapositivas de Ingeniería de Software 2. Está redactado c?mo guía práctica para que un agente de IA pueda identificar el problema de diseño, seleccionar una solución y proponer una implementación orientada a objetos.

---

## Principios varios

### Principios de diseño orientado a objetos

Los principios de diseño son lineamientos para construir soluciones de software más sólidas, robustas, modulares, mantenibles y escalables. En una implementación, el objetivo principal es distribuir correctamente las responsabilidades entre clases y reducir el impacto de los cambios.

**Cómo debe usarlo un agente de IA:**

- Antes de proponer código, identificar qué clase tiene qué responsabilidad.
- Evitar clases que mezclen lógica de negocio, persistencia, interfaz gráfica, validación y comunicación externa.
- Revisar si el diseño permite modificar o extender una funcionalidad sin romper varias partes del sistema.
- Preferir clases pequeñas, con responsabilidades claras, antes que clases enormes que hacen todo.

---

### Bajo acoplamiento

El acoplamiento mide cuánto depende una clase de otras clases. Una clase con alto acoplamiento conoce demasiados detalles de otras clases, por lo que cualquier cambio externo puede obligar a modificarla.

**Problema que resuelve:**

Cuando una clase depende directamente de muchas clases concretas, el sistema se vuelve difícil de mantener, probar y reutilizar.

**Regla para implementar:**

Una clase debe depender solo de lo necesario y, cuando sea posible, depender de abstracciones, interfaces o servicios bien definidos.

**Aplicación práctica:**

- No instanciar muchas clases concretas dentro de una clase de negocio.
- Inyectar dependencias desde fuera cuando sea posible.
- Usar interfaces para representar servicios externos o comportamientos variables.
- Evitar que una clase conozca detalles internos de otra.

**Ejemplo conceptual:**

Mal diseño:

```java
class OrderService {
    private EmailSender emailSender = new EmailSender();
    private MySqlRepository repository = new MySqlRepository();
}
```

Mejor diseño:

```java
class OrderService {
    private final Notifier notifier;
    private final OrderRepository repository;

    public OrderService(Notifier notifier, OrderRepository repository) {
        this.notifier = notifier;
        this.repository = repository;
    }
}
```

---

### Alta cohesión

La cohesión mide qué tan relacionadas están las responsabilidades internas de una clase. Una clase con alta cohesión hace una sola cosa o un conjunto de cosas muy relacionadas.

**Problema que resuelve:**

Una clase con baja cohesión realiza demasiadas tareas, por lo que se vuelve difícil de entender, reutilizar, probar y mantener.

**Regla para implementar:**

Cada clase debe tener una responsabilidad principal claramente definida.

**Aplicación práctica:**

- Separar entidades, servicios, repositorios, validadores, controladores y utilidades.
- Si una clase tiene métodos que no se relacionan entre sí, probablemente debe dividirse.
- Si una clase cambia por muchas razones distintas, tiene baja cohesión.

**Ejemplo conceptual:**

Mal diseño:

```java
class User {
    void saveToDatabase() {}
    void sendWelcomeEmail() {}
    void calculateDiscount() {}
}
```

Mejor diseño:

```java
class User {}
class UserRepository {}
class WelcomeEmailService {}
class DiscountService {}
```

---

### Ley de Demeter

La Ley de Demeter indica: “habla solo con tus amigos, no con extraños”. Un objeto no debería navegar por cadenas largas de objetos para llegar a datos internos.

**Problema que resuelve:**

Evita que una clase dependa de la estructura interna de otras clases. Si el cliente conoce demasiados detalles internos, cualquier cambio en la estructura rompe el código.

**Regla para implementar:**

Una clase debería comunicarse principalmente con:

- Sus propios métodos.
- Objetos recibidos c?mo parámetro.
- Objetos creados localmente.
- Objetos asociados directamente.

**Evitar:**

```java
order.getCustomer().getAddress().getCity();
```

**Preferir:**

```java
order.getCustomerCity();
```

o delegar correctamente:

```java
class Order {
    public String getCustomerCity() {
        return customer.getCity();
    }
}
```

**Cómo debe usarlo un agente de IA:**

- Detectar cadenas de llamadas tipo `a.getB().getC().doSomething()`.
- Proponer métodos intermedios que oculten detalles internos.
- No exponer estructuras internas innecesarias.

---

### DRY — Don’t Repeat Yourself

DRY significa “no te repitas”. Todo conocimiento del sistema debe tener una representación única, inequívoca y autorizada.

**Problema que resuelve:**

Cuando la misma lógica aparece en varios lugares, cualquier cambio debe repetirse manualmente y aumenta el riesgo de errores.

**Regla para implementar:**

Si una fórmula, validación, regla de negocio o algoritmo aparece más de una vez, debe centralizarse.

**Aplicación práctica:**

- Extraer métodos comunes.
- Crear clases reutilizables.
- Usar composición, herencia o servicios compartidos cuando corresponda.
- Centralizar reglas de negocio en un único lugar.

**Ejemplo:**

Mal diseño:

```java
double area1 = Math.PI * r1 * r1;
double area2 = Math.PI * r2 * r2;
```

Mejor diseño:

```java
double calcularAreaCirculo(double radio) {
    return Math.PI * radio * radio;
}
```

---

### KISS — Keep It Simple, Stupid

KISS promueve mantener el diseño simple. La mayoría de sistemas funcionan mejor cuando son fáciles de entender, modificar y probar.

**Problema que resuelve:**

Evita soluciones innecesariamente complejas para problemas simples.

**Regla para implementar:**

Usar la solución más simple que cumpla el requerimiento actual sin sacrificar claridad.

**Aplicación práctica:**

- No crear patrones de diseño si el problema no lo requiere.
- No agregar capas innecesarias.
- No sobreingenierizar ejercicios simples.
- Preferir código directo cuando el cambio futuro no está justificado.

---

### YAGNI — You Ain’t Gonna Need It

YAGNI significa “no lo vas a necesitar”. Indica que no se deben implementar funcionalidades futuras que aún no son requeridas.

**Problema que resuelve:**

Evita gastar tiempo en código no usado, más difícil de mantener y probar.

**Regla para implementar:**

Construir solo lo necesario para los requerimientos actuales.

**Aplicación práctica:**

- No agregar atributos, métodos, clases o configuraciones “por si acaso”.
- No implementar soporte para casos futuros sin requerimiento explícito.
- Diseñar con posibilidad de extensión, pero sin construir funcionalidad innecesaria.

---

### Duck Typing

Duck Typing es un concepto usado en lenguajes dinámicos c?mo Python, Ruby o JavaScript. La idea es que no importa tanto la clase concreta de un objeto, sino si tiene los métodos o atributos necesarios.

**Problema que resuelve:**

Permite escribir código más flexible en lenguajes dinámicos, evitando depender de tipos concretos.

**Regla para implementar:**

Si un objeto tiene el comportamiento esperado, puede usarse, aunque no pertenezca a una jerarquía formal.

**Ejemplo conceptual:**

```python
def saludar_entidad(entidad):
    entidad.saludar()
```

El objeto puede ser `Persona`, `Perro`, `Bot`, etc., siempre que tenga el método `saludar`.

**Advertencia para Java:**

En Java no se usa Duck Typing puro. Se suele modelar con interfaces:

```java
interface Saludable {
    void saludar();
}
```

---

## Principios SOLID

SOLID es un conjunto de principios aplicados a la programación orientada a objetos. Su objetivo es producir código legible, mantenible, testeable, con bajo acoplamiento y alta cohesión.

---

### S — Single Responsibility Principle

**Nombre:** Principio de Responsabilidad Única.

**Idea principal:**

Cada clase debe tener una única responsabilidad dentro del software. Esa responsabilidad debe ser concreta y definida.

**Problema que resuelve:**

Evita clases que hacen demasiadas cosas y que cambian por muchas razones distintas.

**Cómo detectarlo:**

- La clase mezcla varias capas de arquitectura.
- Es difícil de testear.
- Tiene demasiadas líneas o métodos no relacionados.
- Cambia por motivos distintos.

**Regla para implementar:**

Una clase debe tener una sola razón para cambiar.

**Mal diseño:**

```java
class InvoiceService {
    void calculateTotal() {}
    void saveToDatabase() {}
    void sendEmail() {}
    void generatePdf() {}
}
```

**Mejor diseño:**

```java
class InvoiceCalculator {}
class InvoiceRepository {}
class InvoiceEmailService {}
class InvoicePdfGenerator {}
```

**Instrucción para agente de IA:**

Cuando una clase tenga varias responsabilidades, separarla en clases especializadas y conectar esas clases mediante servicios o interfaces.

---

### O — Open/Closed Principle

**Nombre:** Principio Abierto/Cerrado.

**Idea principal:**

Una entidad de software debe estar abierta para extensión, pero cerrada para modificación.

**Problema que resuelve:**

Evita modificar código existente cada vez que se agrega una nueva variante de comportamiento.

**Regla para implementar:**

Agregar nuevas funcionalidades creando nuevas clases, no editando condicionales existentes.

**Mal diseño:**

```java
class DiscountCalculator {
    double calculate(String type, double amount) {
        if (type.equals("VIP")) return amount * 0.8;
        if (type.equals("NORMAL")) return amount;
        return amount;
    }
}
```

**Mejor diseño:**

```java
interface DiscountStrategy {
    double calculate(double amount);
}

class VipDiscount implements DiscountStrategy {
    public double calculate(double amount) {
        return amount * 0.8;
    }
}
```

**Instrucción para agente de IA:**

Si se observan muchos `if`, `switch` o validaciones por tipo, considerar polimorfismo, Strategy, Factory Method o Abstract Factory.

---

### L — Liskov Substitution Principle

**Nombre:** Principio de Sustitución de Liskov.

**Idea principal:**

Una clase hija debe poder sustituir a su clase padre sin romper el comportamiento esperado del programa.

**Problema que resuelve:**

Evita herencias incorrectas donde una subclase no cumple el contrato de la superclase.

**Cómo detectarlo:**

- Una subclase sobrescribe un método para no hacer nada.
- Una subclase lanza excepciones en métodos heredados porque “no aplica”.
- Se necesitan condicionales para tratar subclases c?mo casos especiales.

**Mal diseño:**

```java
class Bird {
    void fly() {}
}

class Penguin extends Bird {
    void fly() {
        throw new UnsupportedOperationException();
    }
}
```

**Mejor diseño:**

```java
interface Bird {}

interface FlyingBird extends Bird {
    void fly();
}

class Eagle implements FlyingBird {
    public void fly() {}
}

class Penguin implements Bird {}
```

**Instrucción para agente de IA:**

No usar herencia solo porque dos entidades se parecen en la vida real. Usar herencia solo si la subclase cumple completamente el comportamiento esperado del padre.

---

### I — Interface Segregation Principle

**Nombre:** Principio de Segregación de Interfaces.

**Idea principal:**

Ninguna clase debería depender de métodos que no usa.

**Problema que resuelve:**

Evita interfaces demasiado grandes, conocidas c?mo `fat interfaces`, que obligan a las clases a implementar métodos vacíos o lanzar errores.

**Regla para implementar:**

Dividir interfaces grandes en interfaces pequeñas y específicas.

**Mal diseño:**

```java
interface Worker {
    void work();
    void eat();
}

class Robot implements Worker {
    public void work() {}
    public void eat() {
        throw new UnsupportedOperationException();
    }
}
```

**Mejor diseño:**

```java
interface Workable {
    void work();
}

interface Eatable {
    void eat();
}

class Robot implements Workable {
    public void work() {}
}

class Human implements Workable, Eatable {
    public void work() {}
    public void eat() {}
}
```

**Instrucción para agente de IA:**

Si una clase implementa métodos que no necesita, dividir la interfaz en interfaces más pequeñas y componerlas según el caso.

---

### D — Dependency Inversion Principle

**Nombre:** Principio de Inversión de Dependencias.

**Idea principal:**

Depende de abstracciones, no de clases concretas.

**Reglas principales:**

- Las clases de alto nivel no deberían depender de clases de bajo nivel.
- Ambas deberían depender de abstracciones.
- Las abstracciones no deberían depender de los detalles.
- Los detalles deberían depender de las abstracciones.

**Problema que resuelve:**

Evita que la lógica importante del sistema quede acoplada a detalles de implementación c?mo bases de datos, archivos, APIs externas, frameworks o servicios concretos.

**Mal diseño:**

```java
class Button {
    private Lamp lamp = new Lamp();

    void press() {
        lamp.turnOn();
    }
}
```

**Mejor diseño:**

```java
interface Switchable {
    void turnOn();
}

class Lamp implements Switchable {
    public void turnOn() {}
}

class Button {
    private final Switchable device;

    public Button(Switchable device) {
        this.device = device;
    }

    void press() {
        device.turnOn();
    }
}
```

**Instrucción para agente de IA:**

Si una clase importante instancia directamente clases concretas, extraer una interfaz e inyectar la dependencia desde fuera.

---

# Patrones

Los patrones de diseño son soluciones reutilizables a problemas recurrentes del diseño orientado a objetos. No son código listo para copiar, sino estructuras de clases, relaciones, responsabilidades y colaboraciones que pueden adaptarse a un contexto.

Un patrón se describe normalmente por:

- Nombre.
- Problema.
- Solución.
- Consecuencias.
- Participantes.
- Colaboraciones.
- Aplicabilidad.

**Regla general para agente de IA:**

No aplicar patrones por obligación. Primero identificar el problema de diseño y luego elegir el patrón que reduzca complejidad, acoplamiento o duplicación.

---

## Patrones Creacionales

Los patrones creacionales establecen cómo deben crearse objetos y clases. Ayudan a que el sistema sea independiente de cómo se crean, componen y representan sus objetos.

---

### Singleton

**Propósito:**

Garantizar que una clase tenga una sola instancia y proporcionar un punto de acceso global a ella.

**Cuándo usarlo:**

- Cuando debe existir exactamente una instancia de una clase.
- Cuando esa instancia debe ser accesible desde un punto conocido.
- Cuando la clase administra un recurso compartido, c?mo configuración, logger o gestor central.

**Estructura:**

- `Singleton`: clase que mantiene una instancia estática privada.
- Constructor privado.
- Método público y estático `getInstance()` para obtener la instancia.

**Implementación base:**

```java
public class Singleton {
    private static Singleton instance;

    private Singleton() {}

    public static Singleton getInstance() {
        if (instance == null) {
            instance = new Singleton();
        }
        return instance;
    }
}
```

**Instrucción para agente de IA:**

Usar Singleton solo si realmente se necesita una instancia única. No usarlo para ocultar variables globales ni para evitar pasar dependencias correctamente.

**Ejemplos:**

- Logger único de aplicación.
- Configuración global.
- Reproductor que solo puede ejecutar un audio a la vez.
- Sistema de archivos centralizado.

---

### Factory Method

**Propósito:**

Definir una interfaz para crear un objeto, pero dejar que las subclases decidan qué clase concreta instanciar.

**Problema que resuelve:**

Una clase necesita crear objetos, pero no puede anticipar qué clase concreta debe crear.

**Cuándo usarlo:**

- Cuando una clase no sabe qué tipo concreto de objeto debe crear.
- Cuando se quiere delegar la creación a subclases.
- Cuando existe una familia de clases con una interfaz común.
- Cuando se quiere evitar `new` directo en la lógica cliente.

**Estructura:**

- `Product`: interfaz o clase abstracta del objeto creado.
- `ConcreteProduct`: implementación concreta del producto.
- `Creator` o `AbstractFactory`: declara el método fábrica.
- `ConcreteCreator`: implementa el método fábrica y devuelve un producto concreto.

**Implementación base:**

```java
interface DBConnection {
    void connect();
}

class MySqlConnection implements DBConnection {
    public void connect() {}
}

class OracleConnection implements DBConnection {
    public void connect() {}
}

abstract class DBManager {
    public abstract DBConnection createConnection();
}

class MySqlDBManager extends DBManager {
    public DBConnection createConnection() {
        return new MySqlConnection();
    }
}

class OracleDBManager extends DBManager {
    public DBConnection createConnection() {
        return new OracleConnection();
    }
}
```

**Instrucción para agente de IA:**

Usar Factory Method cuando el cliente trabaja con una abstracción y la decisión de qué clase concreta crear debe quedar encapsulada en una fábrica o subclase.

**Ejemplos:**

- Conexiones a MySQL u Oracle.
- Creación de documentos según tipo de aplicación.
- DAO para diferentes bases de datos.

---

### Abstract Factory

**Propósito:**

Crear familias de objetos relacionados o dependientes entre sí sin especificar sus clases concretas.

**Diferencia con Factory Method:**

- Factory Method suele crear un producto.
- Abstract Factory crea una familia de productos relacionados.

**Problema que resuelve:**

El sistema debe trabajar con varias familias de objetos y garantizar que los productos de una misma familia se usen juntos.

**Cuándo usarlo:**

- Cuando el sistema debe ser independiente de cómo se crean sus productos.
- Cuando se debe configurar con una familia completa de productos.
- Cuando los productos relacionados deben ser consistentes entre sí.
- Cuando se tienen variantes por plataforma, proveedor, tema visual o arquitectura.

**Estructura:**

- `AbstractFactory`: declara un método de creación por cada tipo de producto.
- `ConcreteFactory`: crea productos de una familia concreta.
- `AbstractProductA`, `AbstractProductB`: interfaces de productos.
- `ConcreteProductA1`, `ConcreteProductB1`: productos concretos de una misma familia.
- `Client`: usa solo interfaces abstractas.

**Implementación base:**

```java
interface Button {
    void render();
}

interface TextBox {
    void render();
}

class WindowsButton implements Button {
    public void render() {}
}

class WindowsTextBox implements TextBox {
    public void render() {}
}

class MacButton implements Button {
    public void render() {}
}

class MacTextBox implements TextBox {
    public void render() {}
}

interface UIFactory {
    Button createButton();
    TextBox createTextBox();
}

class WindowsUIFactory implements UIFactory {
    public Button createButton() {
        return new WindowsButton();
    }

    public TextBox createTextBox() {
        return new WindowsTextBox();
    }
}

class MacUIFactory implements UIFactory {
    public Button createButton() {
        return new MacButton();
    }

    public TextBox createTextBox() {
        return new MacTextBox();
    }
}
```

**Consecuencias:**

- Aísla las clases concretas.
- Facilita cambiar una familia completa de productos.
- Refuerza la consistencia entre productos.
- Puede ser difícil agregar nuevos tipos de productos porque obliga a modificar la interfaz de la fábrica.

**Instrucción para agente de IA:**

Usar Abstract Factory cuando haya varias familias de objetos que deben crearse juntas. No usarlo si solo se crea un único tipo de objeto; en ese caso considerar Factory Method.

**Ejemplos:**

- Componentes UI para Windows y Mac.
- Equipamiento de personajes en un juego: armas y armaduras para mago, arquero o caballero.
- Clientes backend por SOA o REST con familias de objetos relacionadas.

---

## Patrones Estructurales

Los patrones estructurales se enfocan en cómo organizar clases y objetos para formar estructuras más grandes, fáciles de mantener, ampliar y comprender.

---

### Adapter

**Propósito:**

Convertir la interfaz de una clase en otra interfaz esperada por el cliente. Permite que clases con interfaces incompatibles trabajen juntas.

**Problema que resuelve:**

El sistema quiere usar una clase existente, librería externa o API, pero su interfaz no coincide con la que el cliente espera.

**Cuándo usarlo:**

- Cuando se quiere usar una clase existente con interfaz incompatible.
- Cuando se desea integrar una librería externa sin modificar la lógica central.
- Cuando se busca aislar cambios de proveedor o tecnología.
- Cuando dos APIs hacen algo similar pero con métodos, nombres o tipos distintos.

**Estructura:**

- `Client`: usa la interfaz esperada.
- `Target`: interfaz que el cliente conoce.
- `Adapter`: implementa `Target` y traduce llamadas.
- `Adaptee`: clase existente con interfaz incompatible.

**Implementación base:**

```java
interface NotificationTarget {
    void send(String recipient, String content);
}

class FastSMS {
    void sendInstantMessage(String phoneNumber, String messageContent) {}
}

class SMSAdapter implements NotificationTarget {
    private final FastSMS fastSMS;

    public SMSAdapter(FastSMS fastSMS) {
        this.fastSMS = fastSMS;
    }

    public void send(String recipient, String content) {
        fastSMS.sendInstantMessage(recipient, content);
    }
}
```

**Instrucción para agente de IA:**

Usar Adapter cuando el problema sea incompatibilidad de interfaces, no cuando el problema sea simplificar un subsistema completo. Para simplificar subsistemas, usar Facade.

**Ejemplos:**

- Adaptar una librería de SMS a una interfaz de notificaciones.
- Integrar APIs bancarias con formatos distintos.
- Usar un reproductor avanzado desde una interfaz simple.

---

### Facade

**Propósito:**

Proporcionar una interfaz unificada y de alto nivel para un conjunto de interfaces de un subsistema.

**Problema que resuelve:**

El cliente necesita interactuar con muchas clases o subsistemas, generando dependencias, complejidad y acoplamiento.

**Cuándo usarlo:**

- Cuando se necesita una interfaz simple para un subsistema complejo.
- Cuando hay muchas dependencias entre clientes y clases internas.
- Cuando se quiere dividir el sistema en capas.
- Cuando solo algunos clientes avanzados deberían acceder a detalles internos.

**Estructura:**

- `IFacade`: interfaz de alto nivel.
- `DefaultFacadeImpl`: implementación que coordina subsistemas.
- `Subsystems`: clases internas que hacen el trabajo real.
- `Client`: usa la fachada, no los subsistemas directamente.

**Implementación base:**

```java
class Inventory {
    boolean hasStock(String item) { return true; }
}

class Shipping {
    double calculateShipping(int quantity) { return 10.0; }
}

class Discount {
    double calculateDiscount(double subtotal) { return subtotal * 0.1; }
}

class OrderFacade {
    private final Inventory inventory = new Inventory();
    private final Shipping shipping = new Shipping();
    private final Discount discount = new Discount();

    public double placeOrder(String item, int quantity, double subtotal) {
        if (!inventory.hasStock(item)) {
            throw new RuntimeException("Sin stock");
        }

        double shippingCost = shipping.calculateShipping(quantity);
        double discountAmount = discount.calculateDiscount(subtotal);

        return subtotal + shippingCost - discountAmount;
    }
}
```

**Consecuencias:**

- Oculta componentes internos del subsistema.
- Reduce el número de objetos con los que trata el cliente.
- Disminuye el acoplamiento.
- Facilita modificar subsistemas sin afectar clientes.
- Ayuda a estructurar el sistema por capas.

**Instrucción para agente de IA:**

Usar Facade cuando el cliente debe realizar una operación de alto nivel que involucra varios servicios internos.

**Ejemplos:**

- Proceso de compra: inventario, descuentos y envío.
- Pago en línea: banco, facturación, CRM y correo.
- Construcción de robots: cuerpo, color y material.

---

### Decorator

**Propósito:**

Agregar responsabilidades adicionales a un objeto de forma dinámica, c?mo alternativa flexible a la herencia.

**Problema que resuelve:**

La herencia genera demasiadas combinaciones de clases cuando se quieren agregar funcionalidades opcionales o acumulables.

**Cuándo usarlo:**

- Cuando se quiere añadir responsabilidades dinámicamente.
- Cuando las responsabilidades pueden combinarse.
- Cuando se desea retirar o agregar comportamiento en tiempo de ejecución.
- Cuando la herencia produciría explosión de subclases.

**Estructura:**

- `Component`: interfaz común.
- `ConcreteComponent`: objeto base decorable.
- `ComponentDecorator`: clase abstracta que implementa `Component` y contiene un `Component`.
- `ConcreteDecorator`: decorador concreto que agrega comportamiento antes o después de delegar.

**Implementación base:**

```java
interface Beverage {
    String getDescription();
    double cost();
}

class Espresso implements Beverage {
    public String getDescription() {
        return "Espresso";
    }

    public double cost() {
        return 5.0;
    }
}

abstract class BeverageDecorator implements Beverage {
    protected final Beverage beverage;

    public BeverageDecorator(Beverage beverage) {
        this.beverage = beverage;
    }
}

class MilkDecorator extends BeverageDecorator {
    public MilkDecorator(Beverage beverage) {
        super(beverage);
    }

    public String getDescription() {
        return beverage.getDescription() + ", leche";
    }

    public double cost() {
        return beverage.cost() + 0.5;
    }
}
```

**Consecuencias:**

- Más flexible que la herencia.
- Permite añadir y eliminar responsabilidades en tiempo de ejecución.
- Evita clases padre con demasiadas responsabilidades.
- Puede generar muchos objetos pequeños y parecidos.

**Instrucción para agente de IA:**

Usar Decorator cuando el problema sea agregar funcionalidades acumulables a un objeto sin crear una clase por cada combinación.

**Ejemplos:**

- Café con leche, chocolate, caramelo o crema.
- Mensaje convertido a XML, envuelto en SOAP y encriptado.
- Componentes gráficos con bordes o barras de desplazamiento.

---

### Bridge

**Propósito:**

Desacoplar una abstracción de su implementación para que ambas puedan variar independientemente.

**Problema que resuelve:**

La herencia simple obliga a crear una clase por cada combinación entre tipos de abstracción y tipos de implementación.

**Cuándo usarlo:**

- Cuando se quiere evitar una unión permanente entre abstracción e implementación.
- Cuando abstracciones e implementaciones deben extenderse por separado.
- Cuando la implementación puede seleccionarse o cambiarse en tiempo de ejecución.
- Cuando agregar una variante genera muchas combinaciones de clases.

**Estructura:**

- `Abstraction`: interfaz o clase base conocida por el cliente.
- `RefinedAbstraction`: variantes de la abstracción.
- `Implementor`: interfaz de implementación.
- `ConcreteImplementor`: implementaciones concretas.
- La abstracción contiene una referencia a `Implementor`.

**Implementación base:**

```java
interface Device {
    void turnOn();
    void turnOff();
    void setVolume(int volume);
}

class TV implements Device {
    public void turnOn() {}
    public void turnOff() {}
    public void setVolume(int volume) {}
}

class Radio implements Device {
    public void turnOn() {}
    public void turnOff() {}
    public void setVolume(int volume) {}
}

class RemoteControl {
    protected final Device device;

    public RemoteControl(Device device) {
        this.device = device;
    }

    public void turnOn() {
        device.turnOn();
    }

    public void turnOff() {
        device.turnOff();
    }
}

class AdvancedRemoteControl extends RemoteControl {
    public AdvancedRemoteControl(Device device) {
        super(device);
    }

    public void mute() {
        device.setVolume(0);
    }
}
```

**Consecuencias:**

- Desacopla interfaz e implementación.
- Permite configurar implementación en tiempo de ejecución.
- Mejora extensibilidad.
- Permite ampliar abstracciones e implementaciones de forma independiente.

**Instrucción para agente de IA:**

Usar Bridge cuando existan dos dimensiones de variación independientes. Por ejemplo: tipo de control y tipo de dispositivo; tipo de vehículo y tipo de motor; cliente y algoritmo de encriptación.

**Ejemplos:**

- Controles remotos básicos/avanzados y dispositivos TV/radio.
- Vehículos y motores.
- Componente de comunicación y métodos de encriptación.

---

## Patrones de Comportamiento

Los patrones de comportamiento se centran en cómo los objetos interactúan entre sí y cómo se distribuyen las responsabilidades. Su objetivo es facilitar la comunicación entre objetos sin que estén demasiado acoplados.

---

### Strategy

**Propósito:**

Definir una familia de algoritmos, encapsular cada uno y hacerlos intercambiables.

**Problema que resuelve:**

Un objeto necesita cambiar su comportamiento según el contexto, pero no conviene llenar el código de condicionales.

**Cuándo usarlo:**

- Cuando existen varias formas de realizar una operación.
- Cuando se quiere cambiar el algoritmo en tiempo de ejecución.
- Cuando el cliente conoce o necesita elegir entre distintos comportamientos.
- Cuando se quieren evitar muchos `if` o `switch`.

**Estructura:**

- `Context`: clase que usa una estrategia.
- `Strategy`: interfaz común para los algoritmos.
- `ConcreteStrategy`: implementaciones concretas del algoritmo.

**Implementación base:**

```java
interface PaymentStrategy {
    double processPayment(double amount);
}

class CashPayment implements PaymentStrategy {
    public double processPayment(double amount) {
        return amount;
    }
}

class CouponPayment implements PaymentStrategy {
    public double processPayment(double amount) {
        return amount * 0.9;
    }
}

class CardPayment implements PaymentStrategy {
    public double processPayment(double amount) {
        return amount * 1.05;
    }
}

class CashRegister {
    private PaymentStrategy paymentStrategy;

    public void setPaymentStrategy(PaymentStrategy paymentStrategy) {
        this.paymentStrategy = paymentStrategy;
    }

    public double checkout(double amount) {
        return paymentStrategy.processPayment(amount);
    }
}
```

**Consecuencias:**

- Permite cambiar comportamiento dinámicamente.
- Evita heredar o modificar el contexto para cada algoritmo.
- El cliente debe conocer las estrategias disponibles y elegir la adecuada.

**Instrucción para agente de IA:**

Usar Strategy cuando el problema sea intercambiar algoritmos o comportamientos, no cuando el problema sea crear objetos. Si además se necesita crear estrategias según un parámetro, puede combinarse con Factory Method.

**Ejemplos:**

- Tipos de pago: contado, cupón, tarjeta.
- Métodos de autenticación: XML, SQL, memoria.
- Cálculo de feriados por país o grupo religioso.

---

### Command

**Propósito:**

Encapsular una petición en un objeto, permitiendo parametrizar acciones, encolarlas, registrarlas y deshacerlas.

**Problema que resuelve:**

Un invocador necesita ejecutar acciones sin conocer la operación concreta ni el receptor que la realiza.

**Cuándo usarlo:**

- Cuando se necesitan callbacks.
- Cuando se desea encolar órdenes.
- Cuando se quiere ejecutar acciones en diferentes momentos.
- Cuando se necesita historial, deshacer o rehacer.
- Cuando se quiere registrar cambios para recuperarse de una caída.
- Cuando se quiere modelar transacciones.

**Estructura:**

- `ICommand`: interfaz con método `execute()`, y opcionalmente `undo()`.
- `ConcreteCommand`: comando concreto.
- `Receiver`: objeto que sabe realizar la operación.
- `Invoker`: dispara el comando.
- `CommandManager`: registra, busca o administra comandos.
- `Client`: crea comandos y asigna receptores.

**Implementación base:**

```java
interface Command {
    void execute();
    void undo();
}

class Document {
    void copy() {}
    void paste() {}
}

class CopyCommand implements Command {
    private final Document document;

    public CopyCommand(Document document) {
        this.document = document;
    }

    public void execute() {
        document.copy();
    }

    public void undo() {
        // lógica para deshacer si aplica
    }
}

class Button {
    private Command command;

    public void setCommand(Command command) {
        this.command = command;
    }

    public void click() {
        command.execute();
    }
}
```

**Consecuencias:**

- Desacopla el objeto que invoca la operación del objeto que sabe ejecutarla.
- Facilita agregar nuevas órdenes sin cambiar clases existentes.
- Permite historial, cola, undo/redo y logging de operaciones.

**Instrucción para agente de IA:**

Usar Command cuando una acción debe tratarse c?mo objeto. Si el sistema necesita botones, menús, consola, historial o deshacer, Command suele ser adecuado.

**Ejemplos:**

- Editor de texto: copiar, cortar, pegar, deshacer.
- Consola con comandos: exit, echo, date, file, dir, batch.
- Botones o menús de interfaz gráfica.

---

### Iterator

**Propósito:**

Permitir recorrer elementos de una colección sin exponer su representación interna.

**Problema que resuelve:**

El cliente necesita recorrer una colección, pero no debe conocer si internamente es lista, pila, árbol, matriz u otra estructura.

**Cuándo usarlo:**

- Cuando la colección tiene estructura interna compleja.
- Cuando se quiere ocultar la estructura interna por seguridad o conveniencia.
- Cuando se quiere reducir duplicación en código de recorrido.
- Cuando el cliente debe recorrer distintas estructuras sin conocer su tipo concreto.

**Estructura:**

- `Aggregate`: interfaz para colecciones iterables.
- `ConcreteAggregate`: colección concreta.
- `Iterator`: interfaz con métodos c?mo `hasNext()` y `next()`.
- `ConcreteIterator`: implementación concreta del recorrido.
- `Client`: usa el iterador para recorrer.

**Implementación base:**

```java
interface Iterator<T> {
    boolean hasNext();
    T next();
}

interface Aggregate<T> {
    Iterator<T> createIterator();
}

class EmployeeCollection implements Aggregate<Employee> {
    private final List<Employee> employees;

    public EmployeeCollection(List<Employee> employees) {
        this.employees = employees;
    }

    public Iterator<Employee> createIterator() {
        return new EmployeeIterator(employees);
    }
}

class EmployeeIterator implements Iterator<Employee> {
    private final List<Employee> employees;
    private int position = 0;

    public EmployeeIterator(List<Employee> employees) {
        this.employees = employees;
    }

    public boolean hasNext() {
        return position < employees.size();
    }

    public Employee next() {
        return employees.get(position++);
    }
}
```

**Pasos de implementación:**

1. Declarar la interfaz iteradora.
2. Declarar la interfaz de colección con un método para obtener iteradores.
3. Implementar iteradores concretos.
4. Implementar la interfaz de colección en las colecciones concretas.
5. Reemplazar recorridos manuales en el cliente por uso de iteradores.

**Instrucción para agente de IA:**

Usar Iterator cuando el cliente no debe depender de cómo se almacena la colección. Si mañana la estructura cambia de lista a árbol, el cliente no debería cambiar.

**Ejemplos:**

- Recorrer empleados en una jerarquía tipo árbol.
- Recorrer colecciones con estrategias diferentes: profundidad o amplitud.
- Recorrer estructuras desconocidas desde el cliente.

---

# Guía rápida para elegir patrón

| Problema detectado | Patrón/principio recomendado |
|---|---|
| Clase con demasiadas responsabilidades | SRP, alta cohesión |
| Dependencias directas a clases concretas | DIP, bajo acoplamiento |
| Código duplicado | DRY |
| Funcionalidad futura innecesaria | YAGNI |
| Solución demasiado compleja | KISS |
| Cadenas largas de getters | Ley de Demeter |
| Se necesita una única instancia global controlada | Singleton |
| Se debe crear un objeto sin conocer su clase concreta | Factory Method |
| Se debe crear una familia de objetos relacionados | Abstract Factory |
| Interfaz incompatible con la esperada | Adapter |
| Cliente interactúa con muchos subsistemas | Facade |
| Se agregan responsabilidades acumulables a un objeto | Decorator |
| Hay dos jerarquías que varían independientemente | Bridge |
| Se intercambian algoritmos o comportamientos | Strategy |
| Se quiere encapsular una acción c?mo objeto | Command |
| Se recorre una colección sin exponer su estructura | Iterator |
