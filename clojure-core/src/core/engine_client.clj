(ns core.engine-client
  (:import [io.grpc ManagedChannel ManagedChannelBuilder]
           [fish Fish$ImageRequest FishEngineGrpc FishEngineGrpc$FishEngineBlockingStub]
           [java.nio ByteBuffer ByteOrder]))

(defonce channel
  (atom (-> (ManagedChannelBuilder/forAddress "127.0.0.1" 50051)
            (.usePlaintext)
            (.build))))

(defonce stub
  (atom (FishEngineGrpc/newBlockingStub @channel)))

(defn get-tensor-array
  [image-path]
  (let [req (-> (Fish$ImageRequest/newBuilder)
                (.setImagePath image-path)
                (.build))
        res (.getTensor @stub req)
        bytes (.toByteArray (.getTensorData res))
        buffer (.order (ByteBuffer/wrap bytes) ByteOrder/LITTLE_ENDIAN)
        floats (float-array (/ (.capacity buffer) 4))]
    (.get (.asFloatBuffer buffer) floats)
    floats))

(defn analyze-image
  [image-path]
  (let [req (-> (Fish$ImageRequest/newBuilder)
                (.setImagePath image-path)
                (.build))
        res (.analyzeImage @stub req)]
    {:edge-density (.getEdgeDensity res)
     :yellowness    (.getColorYellowness res)
     :saturation    (.getColorSaturation res)
     :fft-ratio     (.getFftLowFreqRatio res)}))
