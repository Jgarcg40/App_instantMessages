from datetime import datetime
import socket
import threading
import mariadb
import queue
import select
import time
from mariadb import Error
import firebase_admin
from firebase_admin import credentials
from firebase_admin import messaging
from dbutils.pooled_db import PooledDB
import hashlib
import os



# Crear un pool de conexiones
pool = PooledDB(
    creator=mariadb,
    host='pon tu ip publica o host aqui',
    port=3306,
    user='pon tu usuario de la BBDD',
    password='pon tu contraseña de la BBDD',
    database='Pon el nombre de tu BBDD',
    mincached=1,
    maxcached=10,
)

# Ruta al archivo JSON de la cuenta de servicio
cred = credentials.Certificate(r'Añade la direccion en tu disco de tu archivo de certificado de firebase')
firebase_admin.initialize_app(cred)

# Diccionario para rastrear conexiones activas de clientes
active_clients = {}

# Cola para almacenar mensajes pendientes
message_queue = queue.Queue()


def cifrar_contrasena(contrasena):
    # Generar una salt aleatoria
    salt = os.urandom(16)

    # Combina la contraseña con la salt y calcula el hash
    contrasena_con_salt = salt + contrasena.encode()
    hashed_contrasena = hashlib.sha256(contrasena_con_salt).hexdigest()

    return salt, hashed_contrasena


def obtener_token_por_usuario(nombre_usuario):
    conn = None  # Asignar None antes del bloque try

    try:
        # Establecer la conexión a la base de datos
        conn = pool.connection()

        # Crear un cursor para ejecutar consultas SQL
        cursor = conn.cursor()

        # Consulta para obtener el token del usuario
        consulta_token = "SELECT token FROM usuarios WHERE nombre_usuario = ?"
        cursor.execute(consulta_token, (nombre_usuario,))
        resultado = cursor.fetchone()

        if resultado:
            token = resultado[0]
            print(f"El token del usuario {nombre_usuario} es: {token}")
            return token  # Retornar el token
        else:
            print(f"No se encontró un token para el usuario {nombre_usuario}")
            return None  # Retornar None si no se encontró un token


    except mariadb.Error as error:

        print(f"Error al obtener el token del usuario: {error}")

        return None

    finally:
        if conn:
            conn.close()
def guardar_token_en_bd(token, nombre_usuario):
    conn = None  # Asignar None antes del bloque try
    try:
        # Establecer la conexión a la base de datos
        conn = pool.connection()
        # Crear un cursor para ejecutar consultas SQL
        cursor = conn.cursor()

        # Verificar si el nombre de usuario ya existe en la base de datos
        consulta_existencia = "SELECT COUNT(*) FROM usuarios WHERE nombre_usuario = ?"
        cursor.execute(consulta_existencia, (nombre_usuario,))
        usuario_existente = cursor.fetchone()[0]

        # Insertar o actualizar el token en la base de datos
        if usuario_existente:
            consulta_actualizacion = "UPDATE usuarios SET token = ? WHERE nombre_usuario = ?"
            cursor.execute(consulta_actualizacion, (token, nombre_usuario))
        else:
            consulta_insercion = "INSERT INTO usuarios (nombre_usuario, token) VALUES (?, ?)"
            cursor.execute(consulta_insercion, (nombre_usuario, token))

        # Confirmar los cambios en la base de datos
        conn.commit()

        print("Token guardado en la base de datos exitosamente.")


    except mariadb.Error as error:

        print(f"Error al guardar el token en la base de datos: {error}")

    finally:

        if conn:
            conn.close()

def send_notification_to_device(token, title, body):
    message = messaging.Message(
        notification=messaging.Notification(title=title, body=body),
        token=token,
    )

    response = messaging.send(message)
    print("Successfully sent message:", response)


def registrar_usuario(nombre, apellido1, apellido2, nombre_usuario, email, contrasena):
    # Conectar a la base de datos
    try:
        # Conectar a la base de datos
        conn = pool.connection()

    except Error as e:
        print(f"Error al conectar a la base de datos: {e}")
        return False

    salt, contrasena_cifrada = cifrar_contrasena(contrasena)
    salt_hex = salt.hex()

    cursor = conn.cursor()

    try:
        # Verificar si el nombre de usuario ya existe en la base de datos
        cursor.execute('SELECT nombre_usuario FROM usuarios WHERE nombre_usuario = %s', (nombre_usuario,))
        user = cursor.fetchone()
        print(user)
        # Si el nombre de usuario ya existe, no se puede registrar de nuevo
        if user:
            return False

        # Insertar el nuevo usuario en la base de datos
        cursor.execute(
            'INSERT INTO usuarios (nombre, apellido1, apellido2, nombre_usuario, email, contraseña, salt) VALUES (%s, %s, %s, %s, %s, %s, %s)',
            (nombre, apellido1, apellido2, nombre_usuario, email, contrasena_cifrada, salt_hex))
        conn.commit()

        return True

    except mariadb.Error as e:
        print(f"Error al registrar usuario: {e}")
        return False
    finally:
        if conn:
            conn.close()


def verificar_credenciales(nombre_usuario, contrasena):
    conn = None
    try:
        # Conectar a la base de datos
        conn = pool.connection()

        # Crear un cursor para ejecutar consultas
        cursor = conn.cursor()

        # Consulta SQL para verificar las credenciales
        query = "SELECT contraseña, salt FROM usuarios WHERE nombre_usuario = ?"
        cursor.execute(query, (nombre_usuario,))

        # Obtener el resultado de la consulta
        result = cursor.fetchone()


        if result:
            # Obtener la contraseña cifrada y la salt almacenada
            contraseña_cifrada = result[0]
            salt_hex = result[1]  # Ya es una cadena
            salt = bytes.fromhex(salt_hex)


            # Calcular el hash de la contraseña proporcionada con la salt almacenada
            contraseña_con_salt = salt + contrasena.encode()
            hashed_contraseña = hashlib.sha256(contraseña_con_salt).hexdigest()

            # Comparar el hash calculado con el hash almacenado
            return contraseña_cifrada == hashed_contraseña
        else:
            return False

    except mariadb.Error as e:
        print(f"Error al conectar a la base de datos: {e}")
        return False
    finally:
        if conn:
            conn.close()
# Función para enviar un mensaje a un cliente
def send_message(destinatario, message, remitente):
    if destinatario in active_clients:
        destinatario_socket = active_clients[destinatario]
        print(f"Enviando mensaje a {destinatario} en dirección IP {destinatario_socket.getpeername()}")
        try:
            message_with_date = f"Mensaje enviado el día: {datetime.now().strftime('%Y-%m-%d')} a las: {datetime.now().strftime('%H:%M')} por {message}"
            destinatario_socket.send(message_with_date.encode())
            print("Mensaje enviado correctamente")
        except Exception as e:
            print(f"Error al enviar el mensaje: {e}")
            # Si ocurre un error al enviar el mensaje, vuelve a encolarlo
            print("Volviendo a encolar el mensaje.")
            message_queue.put((destinatario, message))
    else:

        notification_title = f"Nuevo mensaje de {remitente}"
        notification_body = "Tienes un nuevo mensaje mientras no estabas"
        token_destinatario=obtener_token_por_usuario(destinatario)
        print(token_destinatario)
        send_notification_to_device(token_destinatario, notification_title, notification_body)
        # Si el destinatario no está conectado, almacenar el mensaje en la cola
        print("Destinatario desconectado. Almacenando mensaje en la cola.")
        message_queue.put((destinatario, message,remitente))

# Función para agregar un cliente activo

def add_active_client(nombre_usuario, client_socket):
    active_clients[nombre_usuario] = client_socket

# Función para eliminar un cliente activo
def remove_active_client(nombre_usuario):
    if nombre_usuario in active_clients:
        del active_clients[nombre_usuario]

def process_message_queue():
    while True:
        destinatario, message,remitente = message_queue.get()
        print(f"QUEUE:Extrayendo mensaje de la cola para {destinatario}")
        if destinatario in active_clients:
            print(f"QUEUE:Enviando mensaje a {destinatario} en dirección IP {active_clients[destinatario].getpeername()}")
            send_message(destinatario, message,remitente)
        else:
            print("QUEUE:Destinatario desconectado. Volviendo a encolar el mensaje.")
            message_queue.put((destinatario, message,remitente))

        # Agregar una pausa de 2 segundo para reducir el consumo de CPU
        time.sleep(2)

# Iniciar el hilo para procesar mensajes pendientes
message_thread = threading.Thread(target=process_message_queue)
message_thread.start()
def handle_client(client_socket, client_address):
    
    try:
        print(f"Conexión establecida desde {client_address}")
        nombre_usuario = None  # Inicializar la variable nombre_usuario

        # Recibir la solicitud del cliente
        data = client_socket.recv(1024).decode().strip()
        token, data = data.split("|")
        token = token.rstrip()
        print(f"token: {token}")
        print(f"Solicitud recibida: {data}")
        if data.startswith("REGISTRAR:"):
            # Manejar la solicitud de registro
            _, nombre, apellido1, apellido2, nuevo_usuario, email, nueva_contrasena = data.split(":")
            print(f"Solicitud de registro recibida: {nombre}, {apellido1}, {apellido2}, {nuevo_usuario}, {email}, {nueva_contrasena}")
            registro_exitoso = registrar_usuario(nombre, apellido1, apellido2, nuevo_usuario, email, nueva_contrasena)
            print(registro_exitoso)
            if registro_exitoso:
                client_socket.send(b"OK")
            else:
                client_socket.send(b"ERROR")

        else:
            # Verificar las credenciales del cliente en la base de datos
            contrasena, nombre_usuario = data.split(";")
            nombre_usuario = nombre_usuario.rstrip()
            contrasena = contrasena.rstrip()
            if token == "null":
                print("El token es null, no se guardará en la base de datos")
            else:
                guardar_token_en_bd(token, nombre_usuario)
                print("Token guardado en la base de datos")

            print(f"Nombre de usuario y contraseña recibido: {nombre_usuario}, {contrasena}")
            credenciales_correctas = verificar_credenciales(nombre_usuario, contrasena)
            print(credenciales_correctas)

            # Enviar el resultado de verificación de credenciales al cliente
            if credenciales_correctas:
                client_socket.send(b"OK")
            else:
                client_socket.send(b"ERROR")

        if nombre_usuario is not None:
            add_active_client(nombre_usuario, client_socket)

        while True:
            try:
                # Utilizar select para monitorear los sockets para lectura, escritura y errores
                read_sockets, _, _ = select.select([client_socket], [], [], 1)

                if client_socket in read_sockets:
                    # Decodificar los datos recibidos
                    data = client_socket.recv(1024)
                    if not data:
                        break

                    # Obtener el nombre de usuario del cliente a partir del mensaje recibido
                    message = data.decode()
                    remitente, destinatario, mensaje = message.split("|")
                    remitente = remitente.rstrip()
                    destinatario = destinatario.rstrip()
                    # Almacenar el mensaje en la base de datos (opcional, según tu necesidad)
                    cursor.execute(
                        'INSERT INTO mensajes (remitente_nombre_usuario, destinatario_nombre_usuario, mensaje) VALUES (?, ?, ?)',
                        (remitente, destinatario, mensaje))
                    conn.commit()

                    # Imprimir el mensaje recibido
                    print(f"Mensaje de {remitente} a {destinatario}: {mensaje}")

                    send_message(destinatario, message, remitente)

            except Exception as e:
                print(f"Error en el manejo de mensajes del cliente: {e}")
                break

        while not message_queue.empty():
            destinatario, message, remitente = message_queue.get()
            if destinatario == nombre_usuario:
                message_queue.task_done()  # Marcar el mensaje como procesado
                print(f"QUEUE:Enviando mensaje pendiente a {destinatario}")
                time.sleep(7)
                message_ = f"(¡¡Mensaje enviado cuando estabas desconectado!!) - {message}"

                send_message(destinatario, message_,remitente)
            else:
                message_queue.put((destinatario, message, remitente))  # Volver a encolar el mensaje para otros destinatarios

    finally:

        if nombre_usuario is not None:
            print(nombre_usuario)
            remove_active_client(nombre_usuario)
        if client_socket:
            print("conexión cerrada")
            client_socket.close()


server_host = '0.0.0.0'
server_port = 12345 #puedes poner el que quieras que tengas abierto

# Crear una conexión a la base de datos
try:
    conn = mariadb.connect(
        user='Pon el usuario de tu BBDD',
        password='Pon la contraseña de la BBDD',
        host='Pon tu ip publica o host',
        port=3306,
        database='Pon el nombre de tu BBDD',
        connect_timeout= 31536000 #timeout largo (1año)
    )
    print("Conexión a la base de datos MariaDB exitosa.")
except mariadb.Error as e:
    print(f"Error al conectar con la base de datos MariaDB: {e}")
    exit(1)

# Crear el cursor para la base de datos
cursor = conn.cursor()

# Crear tabla para los usuarios si no existe
cursor.execute('''
  CREATE TABLE IF NOT EXISTS usuarios (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(255),
    apellido1 VARCHAR(255),
    apellido2 VARCHAR(255),
    nombre_usuario VARCHAR(255) UNIQUE,
    email VARCHAR(255) UNIQUE,
    contraseña VARCHAR(255),
    salt VARCHAR(64),
    token VARCHAR(255) UNIQUE
);
''')
conn.commit()

# Crear tabla para los mensajes si no existe
cursor.execute('''
  CREATE TABLE IF NOT EXISTS mensajes (
    id INT AUTO_INCREMENT PRIMARY KEY,
    remitente_nombre_usuario VARCHAR(255),
    destinatario_nombre_usuario VARCHAR(255),
    mensaje TEXT,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (remitente_nombre_usuario) REFERENCES usuarios(nombre_usuario),
    FOREIGN KEY (destinatario_nombre_usuario) REFERENCES usuarios(nombre_usuario)
);
''')
conn.commit()

# Crear el socket del servidor
server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)

# Enlazar el socket del servidor al host y puerto especificado
server_socket.bind((server_host, server_port))

# Escuchar conexiones entrantes (máximo 5 clientes en espera)
server_socket.listen(5)


print(f"Servidor escuchando en {server_host}:{server_port}")


while True:
    # Aceptar conexiones entrantes
    client_socket, client_address = server_socket.accept()

    # Crear un hilo para manejar la conexión del cliente
    client_thread = threading.Thread(target=handle_client, args=(client_socket, client_address))
    client_thread.start()
