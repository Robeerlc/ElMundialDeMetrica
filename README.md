# ⚽ El Mundial de METRICA - Plataforma Oficial de Predicciones 2026

---

## 🎯 1. Objetivo del Proyecto

> Desarrollar una aplicación web corporativa y altamente concurrente para gestionar la "Porra del Mundial 2026" interna de la empresa. Creado en un tiempo de tres semanas, el sistema ofrece una experiencia en tiempo real, conectándose a fuentes de datos deportivas externas, calculando puntuaciones de forma autónoma y fomentando el team building a través de un chat global en vivo.

---

## 💻 2. Stack Tecnológico

* **Frontend:** ![Angular](https://img.shields.io/badge/Angular-DD0031?style=flat-square&logo=angular&logoColor=white) (diseño reactivo, infinite scroll, consumo eficiente de APIs).
* **Backend:** ![Spring Boot](https://img.shields.io/badge/Java_Spring_Boot_4-6DB33F?style=flat-square&logo=spring-boot&logoColor=white) (arquitectura orientada a microservicios junto a WebSocket).
* **Seguridad:** ![JWT](https://img.shields.io/badge/JWT-000000?style=flat-square&logo=json-web-tokens&logoColor=white) Autenticación stateless mediante JWT y delegación ![OAuth2](https://img.shields.io/badge/OAuth2-EB5424?style=flat-square&logo=auth0&logoColor=white) (Microsoft) gestionada por el backend.
* **Persistencia:** ![MySQL](https://img.shields.io/badge/MySQL-4479A1?style=flat-square&logo=mysql&logoColor=white).
* **Integraciones:** Football-Data API v4 (resultados y cronogramas en vivo) y JavaMailSender (activación de cuentas).
* **Infraestructura y Despliegue:** Despliegue continuo (**CI/CD**) en servidor privado utilizando ![Dokploy](https://img.shields.io/badge/Dokploy-6366F1?style=flat-square) para la gestión automatizada de contenedores.

## ✨ 3. Funcionalidades Clave

* **🗂️ Ingesta y Sincronización Automática:** carga automática del calendario de la Copa del Mundo y actualización en vivo de resultados y estados de partido minuto a minuto.
* **🔒 Motor de Predicciones Seguro:** sistema de guardado y edición de apuestas con candado inteligente: se bloquea automáticamente 1 hora antes del pitido inicial. Las apuestas se convierten públicas una vez el partido esté bloqueado para evitar que los usuarios copien a los demás.
* **🏆 Leaderboard:** puntuación automática basada en multiplicadores por fase y criterios estrictos (resultado exacto, diferencia de goles, ganador). Clasificación en vivo con sistema de desempates múltiples.
* **💬 Chat Global:** sala de mensajería corporativa con avatares personalizables y scroll fluido.
