## Guía de decisión: principios y patrones OO

### Cuándo aplicar qué

| Síntoma en el diseño                                                      | Solución                                                        |
| --------------------------------------------------------------------------- | ---------------------------------------------------------------- |
| Clase con muchas responsabilidades o razones para cambiar                   | SRP — separarla en clases especializadas                        |
| Muchos `if`/`switch`según tipo para elegir comportamiento              | OCP + Strategy o Factory Method                                  |
| Subclase que lanza excepción o vacía métodos del padre                   | LSP — reestructurar jerarquía con interfaces más específicas |
| Clase que implementa métodos que no necesita                               | ISP — dividir la interfaz                                       |
| Clase importante que instancia directamente dependencias concretas          | DIP — inyectar abstracciones                                    |
| Lógica duplicada en varios lugares                                         | DRY — extraer método o clase                                   |
| Cadenas `a.getB().getC().doX()`                                           | Ley de Demeter — agregar método intermedio                     |
| Complejidad innecesaria o capas de más                                     | KISS / YAGNI                                                     |
| Se necesita exactamente una instancia global                                | Singleton                                                        |
| Crear un objeto sin conocer su clase concreta                               | Factory Method                                                   |
| Crear una familia de objetos relacionados (ej. UI por plataforma)           | Abstract Factory                                                 |
| Clase existente con interfaz incompatible                                   | Adapter                                                          |
| Cliente depende de muchos subsistemas                                       | Facade                                                           |
| Agregar comportamiento opcional/acumulable sin explotar subclases           | Decorator                                                        |
| Dos jerarquías que varían independientemente (ej. control × dispositivo) | Bridge                                                           |
| Intercambiar algoritmos en tiempo de ejecución                             | Strategy                                                         |
| Encapsular una acción: colas, historial, undo/redo                         | Command                                                          |
| Recorrer colección sin exponer su estructura interna                       | Iterator                                                         |

### Reglas rápidas

* **Preferir interfaces sobre clases concretas** en dependencias.
* **Inyectar dependencias** desde fuera en vez de instanciar con `new`.
* **No aplicar patrones por defecto** — solo si hay un problema real.
* Si el síntoma es  *creación* : Factory Method / Abstract Factory / Singleton.
* Si el síntoma es  *estructura* : Adapter / Facade / Decorator / Bridge.
* Si el síntoma es  *comportamiento* : Strategy / Command / Iterator.
