# Manager Node
Manager node for the Distributed and Decentralized IoT Platform

## API Contract for Manager Node and Client

- Fetch the IFPS hash for a given device ID

`[PUT] /ifps-hash/{device_id}`

Request body:
```json
{
    "email": "<email>",
    "password": "<password>"
}
```

Response:
- For successfully authenticated user - 200 OK with IFPS hash
```json
{
    "ipfsHash": ["<IFPS hash>", ...]
}
```

- For unsuccessfully authenticated user - 401 Unauthorized

## API Contract for Manager Node and Web Portal

- Note: All requests will contain JWT in the Bearer token header

- Register user

`[POST] /users`

Request body:
```json
{
    "email": "<email>",
    "password": "<password>"
}
```

Response:

`200 OK` for successful registration

`400 Bad Request` for failed registration

- Sign in user

`[PUT] /users/signin`

Request body:
```json
{
    "email": "<email>",
    "password": "<password>"
}
```

Response:

`200 OK` for successful sign in
Response body:
```json
{
    "token": "<jwt>"
}
```

`401 Unauthorized` for failed sign in

- Register IoT device

`[POST] /devices`

Request body:
```json
{
    "id": "<id>",
    "name": "<name>",
    "owner": "<owner>",
    "region": "<region>"
}
```

Response:

`200 OK` for successful registration
```json
{
    "deviceID": "<device_id>",
    "apiKey": "<api_key>"
}
```

`400 Bad Request` for failed registration

- Delete IoT device

`[DELETE] /devices/<id>`

Response:

`200 OK` for successful deletion

`400 Bad Request` for failed deletion

- Create access policy

`[POST] /policies`

Request body:
```json
{
    "device_id": "<device_id>",
    "accessing_device_id": ["<accessing_device_id>", ...],
    "accessing_user_id": ["<accessing_user_id>", ...],
}
```

Response:

`200 OK` for successful creation

`400 Bad Request` for failed creation

- Delete access policy

`[DELETE] /policies/device/<device-id>/accessing-device/<accessor-id>`

Response:

`200 OK` for successful deletion

`400 Bad Request` for failed deletion

- Register client

`[POST] /clients`
```json
{
    "email": "<email>",
    "password": "<password>",
}
```

Response:

`200 OK` for successful registration

`400 Bad Request` for failed registration

- Get list of devices

`[GET] /devices`

Response:
    
`200 OK` for successful retrieval
Response body:
```json
{
    "devices": [
        {
            "id": "<id>",
            "name": "<name>",
            "owner": "<owner>",
            "region": "<region>",
            "ipfs": ["<ipfs_hash>", ...]
            "updated_at": "<timestamp>"
        },
        ...
    ]
}
```

`400 Bad Request` for failed retrieval

- Get list of devices

`[GET] /policies`

Response:
    
`200 OK` for successful retrieval
Response body:
```json
{
    "policies": [
        {
            "id": "<id>",
            "name": "<name>",
            "authorized_devices": [
                "<device_id>",
            ],
            "authorized_users": [
                "<user_id>",
            ]
        },
        ...
    ]
}
```

`400 Bad Request` for failed retrieval

## API Contract for Manager Node and IoT Device

- Update IPFS hash

`[PUT] /iot/ifps-hash`

Request body:
```json
{
    "device_id": "<device id>",
    "api_key": "<api key>",
    "ipfs_hash": ["<IPFS hash>", ...]
}
```

Response:
`200 OK` for successful update

`403 Forbidden` for failed update due to device not allowed to update IPFS hash

- Fetch device IPFS hash for other IoT device based on access

`[PUT] iot/data-access`
```json
{
    "device-id": "<device id>",
    "api-key": "<api key>"
}
```

Response:
`200 OK` for successful retrieval with IPFS hash
```json
{
    "ipfs-hash": "<IFPS hash>"
}
```

`401 Unauthorized` if device not allowed access


## Steps to run Manager app

- Add the path to your local fabric-samples directory in FabricService class ("parentPath")
- Navigate to the fabric-gateway-app folder. Build the source code and run the app from this folder.
- Command to build : "mvn clean install"
- Command to run app : "java -jar target/fabric-gateway-app-1.0-SNAPSHOT.jar"
