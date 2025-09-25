export default function Button({label, name, type}: {label: string, name: string, type: string}) {
    return (
        <>
            <p className="input-label">{label}</p>
            <input name={name} type={type} />
        </>
    )
}