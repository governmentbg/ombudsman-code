<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\SoftDeletes;
use Illuminate\Support\Str;

/**
 * @property int    $Pst_id
 * @property int    $created_at
 * @property int    $updated_at
 * @property int    $deleted_at
 * @property Date   $Pst_date
 * @property string $Pst_body
 * @property string $Pst_file
 * @property string $Pst_name
 * @property string $Pst_size
 * @property string $Pst_type
 * @property string $Pst_desc
 */
class MPosition extends Model
{
    use SoftDeletes;
    /**
     * The database table used by the model.
     *
     * @var string
     */
    protected $table = 'm_position';

    /**
     * The primary key for the model.
     *
     * @var string
     */
    protected $primaryKey = 'Pst_id';

    /**
     * Attributes that should be mass-assignable.
     *
     * @var array
     */
    protected $fillable = [
        'Pst_date', 'Pst_path', 'Pst_body', 'Pst_file', 'Pst_name', 'Pst_doc_type', 'Pst_size', 'Pst_type', 'Pst_desc', 'created_at', 'updated_at', 'deleted_at'
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
        'Pst_id' => 'int', 'Pst_date' => 'date', 'Pst_path' => 'string',  'Pst_body' => 'string', 'Pst_file' => 'string', 'Pst_name' => 'string', 'Pst_doc_type' => 'int', 'Pst_size' => 'string', 'Pst_type' => 'string', 'Pst_desc' => 'string', 'created_at' => 'timestamp', 'updated_at' => 'timestamp', 'deleted_at' => 'timestamp'
    ];

    /**
     * The attributes that should be mutated to dates.
     *
     * @var array
     */
    protected $dates = [
        'Pst_date', 'created_at', 'updated_at', 'deleted_at'
    ];

    /**
     * Indicates if the model should be timestamped.
     *
     * @var boolean
     */
    public $timestamps = false;

    public static function boot()
    {
        parent::boot();

        static::creating(function ($article) {

            $article->Pst_path = Str::slug(C2L(substr($article->Pst_name, 0, 80)));

            $article->created_at = now();
            $article->updated_at = now();
        });

        static::saving(function ($article) {
            $article->Pst_path = Str::slug(C2L(substr($article->Pst_name, 0, 80)));
            $article->updated_at = now();
        });
    }

    // Scopes...

    // Functions ...

    // Relations ...
}
