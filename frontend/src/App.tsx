import { RouterProvider } from "react-router-dom";
import { router } from "./router";
import { Providers } from "./app/providers";

function App() {
    return (
        <Providers>
            <RouterProvider router={router} />
        </Providers>
    );
}

export default App;
