package com.manager.app.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import io.grpc.ManagedChannel;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import com.manager.app.model.Asset;
import com.manager.app.model.FabricProperties;
import org.hyperledger.fabric.client.CommitException;
import org.hyperledger.fabric.client.CommitStatusException;
import org.hyperledger.fabric.client.Contract;
import org.hyperledger.fabric.client.EndorseException;
import org.hyperledger.fabric.client.Gateway;
import org.hyperledger.fabric.client.GatewayException;
import org.hyperledger.fabric.client.SubmitException;
import org.hyperledger.fabric.client.identity.Identities;
import org.hyperledger.fabric.client.identity.Identity;
import org.hyperledger.fabric.client.identity.Signer;
import org.hyperledger.fabric.client.identity.Signers;
import org.hyperledger.fabric.client.identity.X509Identity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

@Service
public final class FabricService {
    private final String mspID = "Org1MSP";
    private final String channelName = "mychannel";
    private final String chaincodeName = "basic2";
    private final Path parentPath = Paths.get("/Users/deepakravi/go/src/github.com/sandhya1902/fabric-samples/");
    private Path cryptoPath = parentPath.resolve(Paths.get("test-network", "organizations", "peerOrganizations", "org1.example.com"));
    // Path to user certificate.
    private Path certPath = cryptoPath.resolve(Paths.get("users", "Admin@org1.example.com", "msp", "signcerts", "Admin@org1.example.com-cert.pem"));
    // Path to user private key directory.
    private Path keyDirPath = cryptoPath.resolve(Paths.get("users", "Admin@org1.example.com", "msp", "keystore"));
    // Path to peer tls certificate.
    private Path tlsCertPath = cryptoPath.resolve(Paths.get("peers", "peer0.org1.example.com", "tls", "ca.crt"));

    // Gateway peer end point.
    private String peerEndpoint = "localhost:7051";
    private String overrideAuth = "peer0.org1.example.com";

    //@Autowired
    //private static FabricProperties fabricProperties;
    private final Contract contract;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private ObjectMapper mapper = new ObjectMapper();

    public FabricService() throws Exception {
        // The gRPC client connection should be shared by all Gateway connections to
        // this endpoint.
        var channel = newGrpcConnection();
        var builder = Gateway.newInstance().identity(newIdentity()).signer(newSigner()).connection(channel)
                // Default timeouts for different gRPC calls
                .evaluateOptions(options -> options.withDeadlineAfter(5, TimeUnit.SECONDS))
                .endorseOptions(options -> options.withDeadlineAfter(15, TimeUnit.SECONDS))
                .submitOptions(options -> options.withDeadlineAfter(5, TimeUnit.SECONDS))
                .commitStatusOptions(options -> options.withDeadlineAfter(1, TimeUnit.MINUTES));

        Gateway gateway = builder.connect();
        var network = gateway.getNetwork(channelName);
        // Get the smart contract from the network.
        contract = network.getContract(chaincodeName);
    }

    private ManagedChannel newGrpcConnection() throws IOException, CertificateException {
        var tlsCertReader = Files.newBufferedReader(tlsCertPath);
        var tlsCert = Identities.readX509Certificate(tlsCertReader);

        return NettyChannelBuilder.forTarget(peerEndpoint)
                .sslContext(GrpcSslContexts.forClient().trustManager(tlsCert).build()).overrideAuthority(overrideAuth)
                .build();
    }

    private Identity newIdentity() throws IOException, CertificateException {
        var certReader = Files.newBufferedReader(certPath);
        var certificate = Identities.readX509Certificate(certReader);

        return new X509Identity(mspID, certificate);
    }

    private Signer newSigner() throws IOException, InvalidKeyException {
        var keyReader = Files.newBufferedReader(getPrivateKeyPath());
        var privateKey = Identities.readPrivateKey(keyReader);

        return Signers.newPrivateKeySigner(privateKey);
    }

    private Path getPrivateKeyPath() throws IOException {
        try (var keyFiles = Files.list(keyDirPath)) {
            return keyFiles.findFirst().orElseThrow();
        }
    }

    //TODO fetchIPFSHashForDeviceFromUser returns a string - prettyJson(..) may not work here
    public String createAsset(String owner, String name, String region) {
        System.out.println("\n--> Submit Transaction: createAsset");
        byte[] result;
        try {
            result = contract.submitTransaction("CreateNewDevice", owner, name, region);
            System.out.println("*** createAsset transaction committed successfully");
            System.out.println("result formatted  "+new String(result, StandardCharsets.UTF_8));
            //System.out.println(prettyJson(result));
            return prettyJson(result);
        } catch (EndorseException | SubmitException | CommitStatusException | CommitException e) {
            System.out.println(e.getMessage());
            return "Error while registering new device.";
        }
    }

    public ArrayList<Asset> getAllAssets() {
        System.out.println("\n--> Evaluate Transaction: GetAllAssets");
        try {
            byte[] result = contract.evaluateTransaction("GetAllAssets");
            /*System.out.println("result  "+result);
            System.out.println("result formatted  "+new String(result, StandardCharsets.UTF_8));
            System.out.println("result empty  "+result.toString().isEmpty());
            System.out.println("result blank  "+result.toString().isBlank());
            System.out.println("result formatted blank   "+(new String(result, StandardCharsets.UTF_8)).isBlank());
            System.out.println("result formatted empty  "+(new String(result, StandardCharsets.UTF_8)).isEmpty());
            */String assetString = prettyJson(result);
            /*System.out.println("assetstring  "+assetString);
            System.out.println("assetstring empty  "+assetString.isEmpty());
            System.out.println("assetstring blank  "+assetString.isBlank());
            System.out.println("assetstring null string  "+(assetString == "null"));*/
            if((new String(result, StandardCharsets.UTF_8)).isBlank())
                return new ArrayList<>();
            ArrayList<Asset> assets = Lists.newArrayList(mapper.readValue(assetString, Asset[].class));
            return assets;
        } catch (GatewayException | JsonProcessingException e) {
            System.out.println(e.getMessage());
            return null;
        }
    }

    private String prettyJson(final byte[] json) {
        return prettyJson(new String(json, StandardCharsets.UTF_8));
    }

    private String prettyJson(final String json) {
        var parsedJson = JsonParser.parseString(json);
        return gson.toJson(parsedJson);
    }

    public Asset readAsset(String id) {
        System.out.println("\n--> Evaluate Transaction: ReadAsset");
        try {
            byte[] evaluateResult = contract.evaluateTransaction("ReadAsset", id);
            String assetString = prettyJson(evaluateResult);
            return mapper.readValue(assetString, Asset.class);
        } catch (GatewayException | JsonProcessingException e) {
            System.out.println(e.getMessage());
            return null;
        }
    }

    public String updateAccessPolicy(String deviceId, ArrayList<String> authorizedDevices,
                                    ArrayList<String> authorizedUsers) {
        try {
            System.out.println("\n--> Submit Transaction: updateAccessPolicy");
            // if any parameter is null, set it to empty string
            Asset originalAsset = readAsset(deviceId);
            if (originalAsset == null){
                return "Error updating policy: either this ID doesn't exist or there was an issue on Fabric side while reading asset.";
            }
            originalAsset.authorizedDevices.addAll(authorizedDevices);
            originalAsset.authorizedUsers.addAll(authorizedUsers);
            contract.submitTransaction("UpdateAsset", deviceId, originalAsset.owner, originalAsset.name, originalAsset.region,
                    originalAsset.iPFSHash, new Gson().toJson(originalAsset.authorizedDevices), new Gson().toJson(originalAsset.authorizedUsers));
            System.out.println("******** updateAccessPolicy transaction committed successfully");
            return "Success";
        } catch (EndorseException | SubmitException | CommitStatusException | CommitException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
            return "Error while updating policy";
        }
    }

    //TODO fetchIPFSHashForDeviceFromUser returns a string - prettyJson(..) may not work here
    public String fetchIPFSHashFromUser(String userEmail, String targetDeviceId){
        System.out.println("\n--> Submit Transaction: fetchIPFSHashFromUser");
        try {
            byte[] evaluateResult = contract.evaluateTransaction("fetchIPFSHashForDeviceFromUser", userEmail, targetDeviceId);
            System.out.println("******** fetchIPFSHashFromUser transaction committed successfully");
            System.out.println("result formatted  "+new String(evaluateResult, StandardCharsets.UTF_8));
            return prettyJson(evaluateResult);
        } catch (GatewayException e) {
            System.out.println(e.getMessage());
            return "Error fetching IPFS hash: " + e.getMessage();
        }
    }

    public String fetchIPFSHashFromDevice(String requestingDeviceId, String targetDeviceId){
        System.out.println("\n--> Submit Transaction: fetchIPFSHashFromDevice");
        try {
            byte[] evaluateResult = contract.evaluateTransaction("fetchIPFSHashForDeviceFromUser", requestingDeviceId, targetDeviceId);
            System.out.println("******** fetchIPFSHashFromDevice transaction committed successfully");
            return prettyJson(evaluateResult);
        } catch (GatewayException e) {
            System.out.println(e.getMessage());
            return "Error! Could not fetch IPFS hash for device with ID " + targetDeviceId;
        }
    }

}