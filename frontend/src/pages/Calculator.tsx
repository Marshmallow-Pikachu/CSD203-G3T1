import InputField from "../components/InputField";
import { api } from "../api/client";


export default function Calculator(){
    // const { data, isLoading, error } = useQuery<Tariff[]>({
    //     queryKey: ["tariffs", country],
    //     queryFn: async () => {
    //         const res = await api.get(`/api/v1/tariffs/list?importer=${country}`);
    //         return res.data;
    //     },
    // });

    return (
        <>
        <form>
            <div className="container">
                <h2>Calculator</h2>
                <div className="container-row">
                    <div className="child">
                        <InputField label="Product Description" name="product_description" type="" />
                        <InputField label="Country of Origin" name="" type="" />
                        <InputField label="Product Value (USD)" name="product_value" type="number" />
                        <InputField label="Agreement" name="agreement" type="number" />
                        <InputField label="Product Quantity" name="product_quantity" type="number" />
                    </div>
                    <div className="child">
                        <InputField label="HS Code" name="hs_code" type="number" />
                        <InputField label="Destination Country" name="destination_country" type="" />
                        <InputField label="Shipping Cost (USD)" name="shipping_cost" type="number" />
                        <InputField label="Insurance" name="insurance" type="number" />
                        <div className="container-row">
                            <InputField label="Start Date" name="start_date" type="date" />
                            <InputField label="End Date" name="end_date" type="date" />
                        </div>
                    </div>
                </div>
                <button type="submit">Calculate Landed Cost</button>
            </div>
        </form>
        </>

    )
}