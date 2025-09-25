import InputField from "../components/InputField";


export default function Calculator(){
    return (
        <>
        <form>
            <div className="container">
                <div className="container-row">
                    <div className="child">
                        <InputField label="Product Description" name="product_description" type="" />
                        <InputField label="Country of Origin" name="country_of_origin" type="" />
                        <InputField label="Product Value (USD)" name="product_value" type="number" />
                        <InputField label="Shipping Cost (USD)" name="shipping_cost" type="number" />
                    </div>
                    <div className="child">
                        <InputField label="HS Code" name="hs_code" type="number" />
                        <InputField label="Destination Country" name="destination_country" type="" />
                        <InputField label="Product Quantity" name="product_quantity" type="number" />
                        <InputField label="Insurance" name="insurance" type="number" />
                    </div>
                </div>
                <button type="submit">Calculate Landed Cost</button>
            </div>
        </form>
        </>

    )
}