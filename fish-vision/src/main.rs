use std::net::SocketAddr;
use tonic::{transport::Server, Request, Response, Status};

pub mod fish {
    tonic::include_proto!("fish");
}
use fish::fish_engine_server::{FishEngine, FishEngineServer};
use fish::{AnalysisResponse, ImageRequest, TensorResponse};

// Assuming your lib.rs has `preprocess_image` and `analyze_image` returning the right types
use rust_engine::{analyze_image, preprocess_image};

#[derive(Default)]
pub struct FishEngineService;

#[tonic::async_trait]
impl FishEngine for FishEngineService {
    async fn get_tensor(
        &self,
        request: Request<ImageRequest>,
    ) -> Result<Response<TensorResponse>, Status> {
        let path = request.into_inner().image_path;

        // Run OpenCV in a blocking task to not block the async runtime
        let tensor = tokio::task::spawn_blocking(move || preprocess_image(&path))
            .await
            .map_err(|e| Status::internal(e.to_string()))?
            .map_err(|e| Status::invalid_argument(e.to_string()))?;

        // Convert Vec<f32> to raw bytes
        let bytes: Vec<u8> = tensor.iter().flat_map(|f| f.to_le_bytes()).collect();

        Ok(Response::new(TensorResponse { tensor_data: bytes }))
    }

    async fn analyze_image(
        &self,
        request: Request<ImageRequest>,
    ) -> Result<Response<AnalysisResponse>, Status> {
        let path = request.into_inner().image_path;

        let res = tokio::task::spawn_blocking(move || analyze_image(&path))
            .await
            .map_err(|e| Status::internal(e.to_string()))?
            .map_err(|e| Status::invalid_argument(e.to_string()))?;

        Ok(Response::new(AnalysisResponse {
            edge_density: res.edge_density,
            color_yellowness: res.color_yellowness,
            color_saturation: res.color_saturation,
            fft_low_freq_ratio: res.fft_low_freq_ratio,
        }))
    }
}

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    let addr = SocketAddr::from(([127, 0, 0, 1], 50051));
    let service = FishEngineService::default();

    println!("Rust gRPC Engine listening on {}", addr);
    Server::builder()
        .add_service(FishEngineServer::new(service))
        .serve(addr)
        .await?;

    Ok(())
}
