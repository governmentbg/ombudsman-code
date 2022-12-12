<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\SoftDeletes;

/**
 * @property int    $F_id
 * @property int    $created_at
 * @property int    $deleted_at
 * @property int    $updated_at
 * @property string $F_Family
 * @property string $F_Mail
 * @property string $F_Name
 * @property string $F_Phone
 * @property string $F_Request
 */
class MFeedback extends Model
{
    use SoftDeletes;
    /**
     * The database table used by the model.
     *
     * @var string
     */
    protected $table = 'm_feedback';

    /**
     * The primary key for the model.
     *
     * @var string
     */
    protected $primaryKey = 'F_id';

    /**
     * Attributes that should be mass-assignable.
     *
     * @var array
     */
    protected $fillable = [
        'created_at', 'deleted_at', 'F_Family', 'F_Mail', 'F_Name', 'F_Phone', 'F_Request', 'updated_at'
    ];

    /**
     * The attributes excluded from the model's JSON form.
     *
     * @var array
     */
    protected $hidden = [];

    /**
     * The attributes that should be casted to native types.
     *
     * @var array
     */
    protected $casts = [
        'F_id' => 'int', 'created_at' => 'timestamp', 'deleted_at' => 'timestamp', 'F_Family' => 'string', 'F_Mail' => 'string', 'F_Name' => 'string', 'F_Phone' => 'string', 'F_Request' => 'string', 'updated_at' => 'timestamp'
    ];

    /**
     * The attributes that should be mutated to dates.
     *
     * @var array
     */
    protected $dates = [
        'created_at', 'deleted_at', 'updated_at'
    ];

    /**
     * Indicates if the model should be timestamped.
     *
     * @var boolean
     */
    public $timestamps = true;

    // Scopes...

    // Functions ...

    // Relations ...
}
