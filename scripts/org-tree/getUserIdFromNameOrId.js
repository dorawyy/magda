const isUuid = require("@magda/typescript-common/dist/util/isUuid").default;
async function getUserIdFromNameOrId(nameOrId, pool) {
    if (isUuid(nameOrId)) {
        return nameOrId;
    } else {
        const result = await pool.query(
            `SELECT "id" FROM "users" WHERE "displayName" = $1`,
            [nameOrId]
        );
        if (!result || !result.rows || !result.rows.length) {
            throw new Error(`Cannot locate node record with name: ${nameOrId}`);
        }
        return result.rows[0].id;
    }
}
module.exports = getUserIdFromNameOrId;
