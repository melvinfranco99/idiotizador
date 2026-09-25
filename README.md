# Idiotizador

App Android que captura el audio del micrófono en tiempo real y lo reproduce con **0,5 s de retardo** (ajustable entre 0,1 y 1,5 s). Oír tu propia voz retardada hace que sea casi imposible hablar con fluidez (*delayed auditory feedback*).

## Instalar

Descarga [`idiotizador.apk`](idiotizador.apk) (o desde *Releases*), ábrelo en el móvil y permite la instalación de orígenes desconocidos. Android 6.0 o superior.

## Uso

1. Ponte **auriculares** (sin ellos el altavoz se acopla con el micrófono).
2. Pulsa **EMPEZAR** y concede permiso de micrófono.
3. Habla.

## Compilar

```sh
./gradlew assembleRelease
```

Requiere JDK 17 y Android SDK 34.
